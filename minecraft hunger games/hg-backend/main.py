"""
BTree Hunger Games — FastAPI backend + website.

API routes  → /api/...   (JSON, used by Minecraft plugin and Insomnia)
Web routes  → /          (HTML pages served via Jinja2 templates)
"""

import os
from pathlib import Path

import pymysql
from dotenv import load_dotenv
from fastapi import Depends, FastAPI, Form, Header, HTTPException, Request
from fastapi.responses import HTMLResponse, JSONResponse, RedirectResponse
from fastapi.staticfiles import StaticFiles
from fastapi.templating import Jinja2Templates
from pydantic import BaseModel, EmailStr, HttpUrl

from db import get_db

load_dotenv()

# ---------------------------------------------------------------------------
# App setup
# ---------------------------------------------------------------------------

app = FastAPI(title="BTree Hunger Games API", version="0.1.0")

BASE_DIR = Path(__file__).parent
app.mount("/static", StaticFiles(directory=BASE_DIR / "static"), name="static")
templates = Jinja2Templates(directory=BASE_DIR / "templates")

# ---------------------------------------------------------------------------
# Admin auth
# ---------------------------------------------------------------------------

_ADMIN_KEYS: set[str] = set(
    k.strip() for k in os.getenv("ADMIN_API_KEYS", "").split(",") if k.strip()
)


def require_admin_read(
    x_api_key: str = Header(default=""),
    x_admin_role: str = Header(default=""),
):
    if x_api_key.strip() not in _ADMIN_KEYS:
        raise HTTPException(status_code=401, detail="missing or invalid admin api key")
    if x_admin_role.strip().lower() not in ("admin", "moderator"):
        raise HTTPException(status_code=403, detail="admin or moderator role required")


def require_admin_write(
    x_api_key: str = Header(default=""),
    x_admin_role: str = Header(default=""),
):
    if x_api_key.strip() not in _ADMIN_KEYS:
        raise HTTPException(status_code=401, detail="missing or invalid admin api key")
    if x_admin_role.strip().lower() != "admin":
        raise HTTPException(status_code=403, detail="admin role required")


# ---------------------------------------------------------------------------
# Pydantic models (API)
# ---------------------------------------------------------------------------


class ApplicationIn(BaseModel):
    gamertag: str
    email: EmailStr
    eventCode: str
    source: str = "website"
    notes: str | None = None


class ReceiptIn(BaseModel):
    gamertag: str
    eventCode: str
    receiptCode: str
    screenshotUrl: HttpUrl


class UpdateApplicationIn(BaseModel):
    state: str  # pending | accepted | rejected
    notes: str | None = None


class EventIn(BaseModel):
    code: str
    name: str
    state: str = "planned"
    startsAt: str | None = None


# ---------------------------------------------------------------------------
# Helper
# ---------------------------------------------------------------------------


def _err(status: int, detail: str):
    raise HTTPException(status_code=status, detail=detail)


# ===========================================================================
# WEBSITE ROUTES (HTML)
# ===========================================================================


@app.get("/", response_class=HTMLResponse)
def page_home(request: Request):
    return templates.TemplateResponse("index.html", {"request": request})


@app.get("/apply", response_class=HTMLResponse)
def page_apply_get(request: Request):
    return templates.TemplateResponse(
        "apply.html",
        {
            "request": request,
            "success": request.query_params.get("success"),
            "error": request.query_params.get("error"),
        },
    )


@app.post("/apply", response_class=HTMLResponse)
def page_apply_post(
    request: Request,
    gamertag: str = Form(...),
    email: str = Form(...),
    event_code: str = Form(...),
    notes: str = Form(default=""),
):
    gamertag = gamertag.strip()
    email = email.strip()
    event_code = event_code.strip()
    notes = notes.strip() or None

    try:
        with get_db() as conn:
            with conn.cursor() as cur:
                # Upsert player
                cur.execute(
                    "INSERT INTO players (gamertag, email, status) VALUES (%s, %s, 'applied') "
                    "ON DUPLICATE KEY UPDATE email = VALUES(email)",
                    (gamertag, email),
                )
                cur.execute(
                    "SELECT id FROM players WHERE gamertag = %s LIMIT 1", (gamertag,)
                )
                player = cur.fetchone()

                # Resolve event
                cur.execute(
                    "SELECT id FROM events WHERE code = %s LIMIT 1", (event_code,)
                )
                event = cur.fetchone()
                if not event:
                    return RedirectResponse(
                        f"/apply?error=Event+code+%22{event_code}%22+not+found.+Check+the+Discord+announcement.",
                        status_code=303,
                    )

                # Upsert application
                cur.execute(
                    "INSERT INTO applications (player_id, event_id, state, source, notes) "
                    "VALUES (%s, %s, 'pending', 'website', %s) "
                    "ON DUPLICATE KEY UPDATE source='website', notes=VALUES(notes), updated_at=CURRENT_TIMESTAMP",
                    (player["id"], event["id"], notes),
                )

        return RedirectResponse(f"/apply?success={gamertag}", status_code=303)

    except pymysql.err.IntegrityError as exc:
        msg = str(exc).replace("+", " ")
        return RedirectResponse(f"/apply?error={msg}", status_code=303)


@app.get("/receipt", response_class=HTMLResponse)
def page_receipt_get(request: Request):
    return templates.TemplateResponse(
        "receipt.html",
        {
            "request": request,
            "success": request.query_params.get("success"),
            "error": request.query_params.get("error"),
        },
    )


@app.post("/receipt", response_class=HTMLResponse)
def page_receipt_post(
    request: Request,
    gamertag: str = Form(...),
    event_code: str = Form(...),
    receipt_code: str = Form(...),
    screenshot_url: str = Form(...),
):
    gamertag = gamertag.strip()
    event_code = event_code.strip()
    receipt_code = receipt_code.strip()
    screenshot_url = screenshot_url.strip()

    try:
        with get_db() as conn:
            with conn.cursor() as cur:
                # Resolve player
                cur.execute(
                    "SELECT id FROM players WHERE gamertag = %s LIMIT 1", (gamertag,)
                )
                player = cur.fetchone()
                if not player:
                    return RedirectResponse(
                        "/receipt?error=Gamertag+not+found.+Submit+an+application+first.",
                        status_code=303,
                    )

                # Resolve event
                cur.execute(
                    "SELECT id FROM events WHERE code = %s LIMIT 1", (event_code,)
                )
                event = cur.fetchone()
                if not event:
                    return RedirectResponse(
                        f"/receipt?error=Event+code+%22{event_code}%22+not+found.",
                        status_code=303,
                    )

                # Check receipt code uniqueness
                cur.execute(
                    "SELECT id FROM payments WHERE receipt_code = %s LIMIT 1",
                    (receipt_code,),
                )
                if cur.fetchone():
                    return RedirectResponse(
                        "/receipt?error=This+receipt+code+has+already+been+used.+"
                        "Contact+an+admin+if+you+believe+this+is+a+mistake.",
                        status_code=303,
                    )

                cur.execute(
                    "INSERT INTO payments (player_id, event_id, receipt_code, screenshot_url, status) "
                    "VALUES (%s, %s, %s, %s, 'pending')",
                    (player["id"], event["id"], receipt_code, screenshot_url),
                )

        return RedirectResponse(f"/receipt?success={gamertag}", status_code=303)

    except pymysql.err.IntegrityError:
        return RedirectResponse(
            "/receipt?error=This+receipt+code+has+already+been+used.+"
            "Contact+an+admin+if+you+believe+this+is+a+mistake.",
            status_code=303,
        )


@app.get("/players", response_class=HTMLResponse)
def page_players(request: Request):
    with get_db() as conn:
        with conn.cursor() as cur:
            cur.execute(
                "SELECT gamertag, status, created_at FROM players "
                "WHERE status = 'approved' ORDER BY gamertag ASC"
            )
            players = cur.fetchall()
    return templates.TemplateResponse(
        "players.html", {"request": request, "players": players}
    )


@app.get("/plugins", response_class=HTMLResponse)
def page_plugins(request: Request):
    return templates.TemplateResponse("plugins.html", {"request": request})


@app.get("/discord", response_class=HTMLResponse)
def page_discord(request: Request):
    return templates.TemplateResponse("discord.html", {"request": request})


@app.get("/contact", response_class=HTMLResponse)
def page_contact(request: Request):
    return templates.TemplateResponse("contact.html", {"request": request})


# ===========================================================================
# API ROUTES (JSON)
# ===========================================================================


@app.get("/api/health")
def api_health():
    return {"status": "ok"}


@app.post("/api/applications", status_code=201)
def api_create_application(body: ApplicationIn):
    try:
        with get_db() as conn:
            with conn.cursor() as cur:
                cur.execute(
                    "INSERT INTO players (gamertag, email, status) VALUES (%s, %s, 'applied') "
                    "ON DUPLICATE KEY UPDATE email = VALUES(email)",
                    (body.gamertag, body.email),
                )
                cur.execute(
                    "SELECT id, gamertag, email, status FROM players WHERE gamertag = %s LIMIT 1",
                    (body.gamertag,),
                )
                player = cur.fetchone()

                cur.execute(
                    "SELECT id, code FROM events WHERE code = %s LIMIT 1",
                    (body.eventCode,),
                )
                event = cur.fetchone()
                if not event:
                    _err(400, "eventCode not found")

                cur.execute(
                    "INSERT INTO applications (player_id, event_id, state, source, notes) "
                    "VALUES (%s, %s, 'pending', %s, %s) "
                    "ON DUPLICATE KEY UPDATE source=VALUES(source), notes=VALUES(notes), updated_at=CURRENT_TIMESTAMP",
                    (player["id"], event["id"], body.source, body.notes),
                )
                cur.execute(
                    "SELECT id, player_id, event_id, state, source, notes, created_at, updated_at "
                    "FROM applications WHERE player_id = %s AND event_id = %s LIMIT 1",
                    (player["id"], event["id"]),
                )
                application = cur.fetchone()

        return {"player": player, "application": application}
    except HTTPException:
        raise
    except Exception as exc:
        _err(500, str(exc))


@app.post("/api/payments/receipt", status_code=201)
def api_submit_receipt(body: ReceiptIn):
    try:
        with get_db() as conn:
            with conn.cursor() as cur:
                cur.execute(
                    "SELECT id FROM players WHERE gamertag = %s LIMIT 1",
                    (body.gamertag,),
                )
                player = cur.fetchone()
                if not player:
                    _err(404, "gamertag not found — submit an application first")

                cur.execute(
                    "SELECT id FROM events WHERE code = %s LIMIT 1",
                    (body.eventCode,),
                )
                event = cur.fetchone()
                if not event:
                    _err(400, "eventCode not found")

                cur.execute(
                    "SELECT id FROM payments WHERE receipt_code = %s LIMIT 1",
                    (str(body.receiptCode),),
                )
                if cur.fetchone():
                    _err(
                        409,
                        "This receipt code has already been used. "
                        "Contact an admin if you believe this is a mistake.",
                    )

                cur.execute(
                    "INSERT INTO payments (player_id, event_id, receipt_code, screenshot_url, status) "
                    "VALUES (%s, %s, %s, %s, 'pending')",
                    (
                        player["id"],
                        event["id"],
                        str(body.receiptCode),
                        str(body.screenshotUrl),
                    ),
                )
                cur.execute(
                    "SELECT id, player_id, event_id, receipt_code, screenshot_url, status, created_at "
                    "FROM payments WHERE receipt_code = %s LIMIT 1",
                    (str(body.receiptCode),),
                )
                payment = cur.fetchone()

        return payment
    except HTTPException:
        raise
    except Exception as exc:
        _err(500, str(exc))


@app.get("/api/players/approved")
def api_players_approved():
    with get_db() as conn:
        with conn.cursor() as cur:
            cur.execute(
                "SELECT gamertag, status, created_at FROM players "
                "WHERE status = 'approved' ORDER BY gamertag ASC"
            )
            players = cur.fetchall()
    return {"count": len(players), "players": players}


@app.get("/api/players/status/{gamertag}")
def api_player_status(gamertag: str):
    with get_db() as conn:
        with conn.cursor() as cur:
            cur.execute(
                "SELECT p.gamertag, p.status AS player_status, "
                "       a.state AS application_state, pay.status AS payment_status, "
                "       e.code AS event_code "
                "FROM players p "
                "LEFT JOIN applications a ON a.player_id = p.id "
                "LEFT JOIN events e       ON e.id = a.event_id "
                "LEFT JOIN payments pay   ON pay.player_id = p.id AND pay.event_id = e.id "
                "WHERE LOWER(p.gamertag) = LOWER(%s) "
                "ORDER BY a.created_at DESC LIMIT 1",
                (gamertag,),
            )
            row = cur.fetchone()
    if not row:
        _err(404, "player not found")
    return row


@app.get("/api/admin/players", dependencies=[Depends(require_admin_read)])
def api_admin_list_players(request: Request):
    status_filter = request.query_params.get("status")
    with get_db() as conn:
        with conn.cursor() as cur:
            if status_filter:
                cur.execute(
                    "SELECT p.id AS player_id, p.gamertag, p.email, p.status AS player_status, "
                    "       p.created_at, a.id AS application_id, a.state AS application_state, "
                    "       a.source, a.notes AS application_notes, a.updated_at, "
                    "       e.code AS event_code, "
                    "       pay.id AS payment_id, pay.receipt_code, pay.screenshot_url, "
                    "       pay.status AS payment_status, pay.verified_by, pay.verified_at "
                    "FROM players p "
                    "LEFT JOIN applications a ON a.player_id = p.id "
                    "LEFT JOIN events e       ON e.id = a.event_id "
                    "LEFT JOIN payments pay   ON pay.player_id = p.id AND pay.event_id = e.id "
                    "WHERE p.status = %s "
                    "ORDER BY p.created_at DESC LIMIT 500",
                    (status_filter,),
                )
            else:
                cur.execute(
                    "SELECT p.id AS player_id, p.gamertag, p.email, p.status AS player_status, "
                    "       p.created_at, a.id AS application_id, a.state AS application_state, "
                    "       a.source, a.notes AS application_notes, a.updated_at, "
                    "       e.code AS event_code, "
                    "       pay.id AS payment_id, pay.receipt_code, pay.screenshot_url, "
                    "       pay.status AS payment_status, pay.verified_by, pay.verified_at "
                    "FROM players p "
                    "LEFT JOIN applications a ON a.player_id = p.id "
                    "LEFT JOIN events e       ON e.id = a.event_id "
                    "LEFT JOIN payments pay   ON pay.player_id = p.id AND pay.event_id = e.id "
                    "ORDER BY p.created_at DESC LIMIT 500"
                )
            rows = cur.fetchall()
    return {"count": len(rows), "players": rows}


@app.patch("/api/admin/applications/{app_id}", dependencies=[Depends(require_admin_write)])
def api_admin_update_application(app_id: int, body: UpdateApplicationIn):
    if body.state not in ("pending", "accepted", "rejected"):
        _err(400, "state must be pending, accepted, or rejected")

    with get_db() as conn:
        with conn.cursor() as cur:
            cur.execute(
                "UPDATE applications "
                "SET state = %s, notes = COALESCE(%s, notes), updated_at = CURRENT_TIMESTAMP "
                "WHERE id = %s",
                (body.state, body.notes, app_id),
            )
            if cur.rowcount == 0:
                _err(404, "application not found")

            if body.state == "accepted":
                cur.execute(
                    "UPDATE players p JOIN applications a ON a.player_id = p.id "
                    "SET p.status = 'approved' WHERE a.id = %s",
                    (app_id,),
                )

            cur.execute(
                "SELECT a.id, a.player_id, a.event_id, a.state, a.source, a.notes, "
                "       a.created_at, a.updated_at, "
                "       p.gamertag, p.email, p.status AS player_status, e.code AS event_code "
                "FROM applications a "
                "JOIN players p ON p.id = a.player_id "
                "JOIN events e  ON e.id = a.event_id "
                "WHERE a.id = %s LIMIT 1",
                (app_id,),
            )
            row = cur.fetchone()

    return row


@app.post("/api/admin/events", status_code=201, dependencies=[Depends(require_admin_write)])
def api_admin_create_event(body: EventIn):
    with get_db() as conn:
        with conn.cursor() as cur:
            cur.execute(
                "INSERT INTO events (code, name, state, starts_at) VALUES (%s, %s, %s, %s) "
                "ON DUPLICATE KEY UPDATE name=VALUES(name), state=VALUES(state), starts_at=VALUES(starts_at)",
                (body.code, body.name, body.state, body.startsAt),
            )
            cur.execute(
                "SELECT id, code, name, state, starts_at, created_at FROM events WHERE code = %s LIMIT 1",
                (body.code,),
            )
            event = cur.fetchone()
    return event


@app.get("/api/admin/events", dependencies=[Depends(require_admin_read)])
def api_admin_list_events(request: Request):
    state_filter = request.query_params.get("state")
    with get_db() as conn:
        with conn.cursor() as cur:
            if state_filter:
                cur.execute(
                    "SELECT id, code, name, state, starts_at, created_at FROM events "
                    "WHERE state = %s ORDER BY COALESCE(starts_at, created_at) DESC LIMIT 100",
                    (state_filter,),
                )
            else:
                cur.execute(
                    "SELECT id, code, name, state, starts_at, created_at FROM events "
                    "ORDER BY COALESCE(starts_at, created_at) DESC LIMIT 100"
                )
            events = cur.fetchall()
    return {"count": len(events), "events": events}


@app.get("/api/admin/events/{code}/whitelist", dependencies=[Depends(require_admin_read)])
def api_admin_whitelist(code: str):
    with get_db() as conn:
        with conn.cursor() as cur:
            cur.execute(
                "SELECT id, code, name, state FROM events WHERE code = %s LIMIT 1",
                (code,),
            )
            event = cur.fetchone()
            if not event:
                _err(404, "event not found")

            cur.execute(
                "SELECT DISTINCT p.gamertag "
                "FROM players p "
                "JOIN applications a ON a.player_id = p.id "
                "JOIN events e        ON e.id = a.event_id "
                "JOIN payments pay    ON pay.player_id = p.id AND pay.event_id = e.id "
                "WHERE e.code = %s AND a.state = 'accepted' AND pay.status = 'accepted' "
                "ORDER BY p.gamertag",
                (code,),
            )
            rows = cur.fetchall()

    return {
        "event": {"code": event["code"], "name": event["name"], "state": event["state"]},
        "count": len(rows),
        "gamertags": [r["gamertag"] for r in rows],
    }


# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------

if __name__ == "__main__":
    import uvicorn

    uvicorn.run("main:app", host="0.0.0.0", port=int(os.getenv("PORT", 8000)), reload=True)
