/* global starfield, nav scroll, mobile hamburger */

(function () {
    'use strict';

    /* ── STARFIELD ─────────────────────────────────────── */
    const canvas = document.getElementById('starfield');
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    let stars = [];
    const STAR_COUNT = 220;

    function resize() {
        canvas.width  = window.innerWidth;
        canvas.height = window.innerHeight;
    }

    function buildStars() {
        stars = [];
        for (let i = 0; i < STAR_COUNT; i++) {
            stars.push({
                x:           Math.random() * canvas.width,
                y:           Math.random() * canvas.height,
                r:           Math.random() * 1.4 + 0.2,
                opacity:     Math.random(),
                twinkleRate: Math.random() * 0.018 + 0.004,
                twinkleDir:  Math.random() > 0.5 ? 1 : -1,
            });
        }
    }

    function drawStars() {
        ctx.clearRect(0, 0, canvas.width, canvas.height);
        for (const s of stars) {
            s.opacity += s.twinkleRate * s.twinkleDir;
            if (s.opacity >= 1)   { s.opacity = 1;   s.twinkleDir = -1; }
            if (s.opacity <= 0.08){ s.opacity = 0.08; s.twinkleDir =  1; }

            ctx.beginPath();
            ctx.arc(s.x, s.y, s.r, 0, Math.PI * 2);
            ctx.fillStyle = `rgba(255,255,255,${s.opacity})`;
            ctx.fill();
        }
        requestAnimationFrame(drawStars);
    }

    window.addEventListener('resize', () => { resize(); buildStars(); });
    resize();
    buildStars();
    drawStars();

    /* ── NAVBAR SCROLL EFFECT ───────────────────────────── */
    const navbar = document.querySelector('.navbar');
    window.addEventListener('scroll', () => {
        if (window.scrollY > 60) {
            navbar.style.borderBottomColor = 'rgba(255,232,31,0.35)';
        } else {
            navbar.style.borderBottomColor = 'rgba(255,232,31,0.15)';
        }
    }, { passive: true });

    /* ── MOBILE HAMBURGER ───────────────────────────────── */
    const hamburger = document.querySelector('.nav-hamburger');
    const navLinks  = document.querySelector('.nav-links');

    if (hamburger && navLinks) {
        hamburger.addEventListener('click', () => {
            navLinks.classList.toggle('open');
        });

        // close menu when a link is clicked
        navLinks.querySelectorAll('a').forEach(link => {
            link.addEventListener('click', () => {
                navLinks.classList.remove('open');
            });
        });
    }

    /* ── SMOOTH SCROLL ──────────────────────────────────── */
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function (e) {
            const target = document.querySelector(this.getAttribute('href'));
            if (target) {
                e.preventDefault();
                target.scrollIntoView({ behavior: 'smooth', block: 'start' });
            }
        });
    });

    /* ── SCROLL-IN ANIMATIONS ────────────────────────────── */
    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add('visible');
                observer.unobserve(entry.target);
            }
        });
    }, { threshold: 0.12 });

    document.querySelectorAll(
        '.lego-brick, .quest-entry, .mtg-card, .mc-slot, .holo-panel'
    ).forEach(el => {
        el.classList.add('fade-in');
        observer.observe(el);
    });

}());
