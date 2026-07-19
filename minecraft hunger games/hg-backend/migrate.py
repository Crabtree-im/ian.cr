"""Run schema.sql against the configured MySQL database."""
import os
from pathlib import Path

import pymysql
from dotenv import load_dotenv

load_dotenv()

import re

schema = Path(__file__).parent / "schema.sql"
raw = schema.read_text()

# Strip -- comments before splitting so a leading comment doesn't swallow a CREATE TABLE
sql = re.sub(r"--[^\n]*", "", raw)

conn = pymysql.connect(
    host=os.getenv("DB_HOST", "127.0.0.1"),
    port=int(os.getenv("DB_PORT", 3306)),
    user=os.getenv("DB_USER", "root"),
    password=os.getenv("DB_PASSWORD", ""),
    database=os.getenv("DB_NAME", "hungergames"),
    autocommit=True,
)

cursor = conn.cursor()
for statement in sql.split(";"):
    stmt = statement.strip()
    if not stmt or stmt.startswith("--"):
        continue
    try:
        cursor.execute(stmt)
        print(f"OK: {stmt[:60].replace(chr(10), ' ')}...")
    except pymysql.err.OperationalError as exc:
        code, msg = exc.args
        if code == 1061:  # Duplicate key name — table already has the index
            print(f"SKIP (index exists): {stmt[:60]}...")
        else:
            raise

cursor.close()
conn.close()
print("\nMigration complete.")
