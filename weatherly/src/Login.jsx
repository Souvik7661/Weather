import { useEffect, useRef, useState } from 'react';
import { AnimatePresence, motion, useAnimationControls } from 'framer-motion';
import { ArrowLeft, ArrowRight, Check, Cloud, Droplet, Leaf, Phone, ShieldCheck } from 'lucide-react';
import Scene from './Scene.jsx';
import './login.css';

/* ------------------------------------------------------------------ */
/*  Auth stubs – replace with your real backend / Firebase / Auth0     */
/* ------------------------------------------------------------------ */
const DEMO_OTP = '123456';
const wait = (ms) => new Promise((r) => setTimeout(r, ms));

// Firebase: signInWithPhoneNumber(auth, `${dial}${digits}`, recaptcha)
async function sendOtp() { await wait(1000); return true; }
// Firebase: confirmationResult.confirm(code)
async function verifyOtp(code) { await wait(900); return code === DEMO_OTP; }
// Firebase: signInWithPopup(auth, new GoogleAuthProvider())
async function googleSignIn() { await wait(1200); return { method: 'google', name: 'Google user' }; }

const COUNTRIES = [
  { code: 'IN', flag: '🇮🇳', dial: '+91', len: 10 },
  { code: 'US', flag: '🇺🇸', dial: '+1', len: 10 },
  { code: 'GB', flag: '🇬🇧', dial: '+44', len: 10 },
  { code: 'AU', flag: '🇦🇺', dial: '+61', len: 9 },
  { code: 'AE', flag: '🇦🇪', dial: '+971', len: 9 },
  { code: 'SG', flag: '🇸🇬', dial: '+65', len: 8 },
];

const GoogleLogo = () => (
  <svg width="20" height="20" viewBox="0 0 48 48" aria-hidden="true">
    <path fill="#FFC107" d="M43.6 20.1H42V20H24v8h11.3C33.7 32.7 29.2 36 24 36c-6.6 0-12-5.4-12-12s5.4-12 12-12c3.1 0 5.8 1.2 8 3.1l5.7-5.7C34 6.1 29.3 4 24 4 13 4 4 13 4 24s9 20 20 20 20-9 20-20c0-1.3-.1-2.6-.4-3.9z" />
    <path fill="#FF3D00" d="M6.3 14.7l6.6 4.8C14.7 15.1 19 12 24 12c3.1 0 5.8 1.2 8 3.1l5.7-5.7C34 6.1 29.3 4 24 4 16.3 4 9.7 8.3 6.3 14.7z" />
    <path fill="#4CAF50" d="M24 44c5.2 0 9.9-2 13.4-5.2l-6.2-5.2C29.2 35.1 26.7 36 24 36c-5.2 0-9.6-3.3-11.3-8l-6.5 5C9.5 39.6 16.2 44 24 44z" />
    <path fill="#1976D2" d="M43.6 20.1H42V20H24v8h11.3c-.8 2.2-2.2 4.2-4.1 5.6l6.2 5.2C37 39.2 44 34 44 24c0-1.3-.1-2.6-.4-3.9z" />
  </svg>
);

const card = {
  hidden: { opacity: 0, y: 40, scale: 0.96 },
  show: { opacity: 1, y: 0, scale: 1, transition: { type: 'spring', stiffness: 90, damping: 16, staggerChildren: 0.08, delayChildren: 0.15 } },
};
const item = { hidden: { opacity: 0, y: 16 }, show: { opacity: 1, y: 0, transition: { type: 'spring', stiffness: 130, damping: 16 } } };
const slide = {
  initial: (d) => ({ opacity: 0, x: 40 * d }),
  animate: { opacity: 1, x: 0, transition: { type: 'spring', stiffness: 160, damping: 20 } },
  exit: (d) => ({ opacity: 0, x: -40 * d, transition: { duration: 0.18 } }),
};

/* floating weather chips on the brand panel */
const CHIPS = [
  { Icon: Droplet, color: '#59a8ff', text: 'Humidity 78%', style: { top: '14%', right: '8%' }, d: 0 },
  { Icon: Leaf, color: '#3be59a', text: 'AQI 78 · Good', style: { bottom: '30%', left: '6%' }, d: 1.2 },
];

export default function Login({ onLogin }) {
  const [step, setStep] = useState('phone'); // phone | otp | done
  const [dir, setDir] = useState(1);
  const [ci, setCi] = useState(0);
  const [digits, setDigits] = useState('');
  const [otp, setOtp] = useState(['', '', '', '', '', '']);
  const [busy, setBusy] = useState(null); // send | verify | google
  const [error, setError] = useState('');
  const [left, setLeft] = useState(0);
  const [user, setUser] = useState(null);
  const refs = useRef([]);
  const shake = useAnimationControls();
  const country = COUNTRIES[ci];
  const valid = digits.length === country.len;
  const shown = digits.length > 5 ? `${digits.slice(0, 5)} ${digits.slice(5)}` : digits;

  /* resend countdown */
  useEffect(() => {
    if (step !== 'otp' || left <= 0) return undefined;
    const id = setTimeout(() => setLeft((s) => s - 1), 1000);
    return () => clearTimeout(id);
  }, [step, left]);

  /* finish -> hand over to the app */
  useEffect(() => {
    if (step !== 'done') return undefined;
    const id = setTimeout(() => onLogin(user), 1600);
    return () => clearTimeout(id);
  }, [step, user, onLogin]);

  const fail = (msg) => { setError(msg); shake.start({ x: [0, -10, 10, -8, 8, 0], transition: { duration: 0.45 } }); };

  const send = async (e) => {
    e?.preventDefault();
    if (!valid || busy) return;
    setBusy('send'); setError('');
    try {
      await sendOtp(`${country.dial}${digits}`);
      setDir(1); setStep('otp'); setLeft(30); setOtp(['', '', '', '', '', '']);
      setTimeout(() => refs.current[0]?.focus(), 350);
    } catch { fail('Could not send the code. Try again.'); }
    setBusy(null);
  };

  const verify = async (code) => {
    setBusy('verify'); setError('');
    try {
      const ok = await verifyOtp(code);
      if (ok) { setUser({ method: 'phone', phone: `${country.dial}${digits}` }); setDir(1); setStep('done'); } else {
        fail('That code is not right. Check and try again.');
        setOtp(['', '', '', '', '', '']);
        refs.current[0]?.focus();
      }
    } catch { fail('Verification failed. Try again.'); }
    setBusy(null);
  };

  const setDigit = (i, v) => {
    const d = v.replace(/\D/g, '').slice(-1);
    const next = [...otp]; next[i] = d; setOtp(next); setError('');
    if (d && i < 5) refs.current[i + 1]?.focus();
    if (next.every(Boolean)) verify(next.join(''));
  };
  const onKey = (i, e) => {
    if (e.key === 'Backspace' && !otp[i] && i > 0) { const next = [...otp]; next[i - 1] = ''; setOtp(next); refs.current[i - 1]?.focus(); }
    if (e.key === 'ArrowLeft' && i > 0) refs.current[i - 1]?.focus();
    if (e.key === 'ArrowRight' && i < 5) refs.current[i + 1]?.focus();
  };
  const onPaste = (e) => {
    const p = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, 6);
    if (!p) return;
    e.preventDefault();
    const next = ['', '', '', '', '', ''];
    p.split('').forEach((c, i) => { next[i] = c; });
    setOtp(next);
    refs.current[Math.min(p.length, 5)]?.focus();
    if (p.length === 6) verify(p);
  };

  const google = async () => {
    if (busy) return;
    setBusy('google'); setError('');
    try { const u = await googleSignIn(); setUser(u); setDir(1); setStep('done'); } catch { fail('Google sign-in was cancelled.'); }
    setBusy(null);
  };

  const back = () => { setDir(-1); setStep('phone'); setError(''); };

  return (
    <>
      <Scene phase="dusk" progress={0.97} />
      <div className="vignette" />
      <main className="loginShell">
        <motion.div className="glass loginCard" variants={card} initial="hidden" animate="show">
          {/* ---------- brand pane ---------- */}
          <aside className="brandPane">
            <motion.div variants={item} className="brandLogo">
              <motion.span animate={{ y: [0, -3, 0] }} transition={{ duration: 4, repeat: Infinity, ease: 'easeInOut' }}><Cloud size={40} fill="#8aa6ff" color="#cfe0ff" strokeWidth={1.4} /></motion.span>
              <div><b>Weatherly</b><small>See Beyond the Sky</small></div>
            </motion.div>

            <motion.div variants={item} className="orb partly brandOrb">
              <motion.div className="sunBall" animate={{ scale: [1, 1.06, 1] }} transition={{ duration: 4, repeat: Infinity, ease: 'easeInOut' }} />
              <motion.div className="cloudWrap" animate={{ y: [0, -7, 0], x: [0, 5, 0] }} transition={{ duration: 6, repeat: Infinity, ease: 'easeInOut' }}>
                <div className="cloudShape"><i /><i /><i /><b /></div>
              </motion.div>
              {CHIPS.map(({ Icon, color, text, style, d }) => (
                <motion.div key={text} className="floatChip" style={style} animate={{ y: [0, -8, 0] }} transition={{ duration: 5, repeat: Infinity, ease: 'easeInOut', delay: d }}>
                  <Icon size={16} color={color} />{text}
                </motion.div>
              ))}
            </motion.div>

            <motion.div variants={item}>
              <h2>Forecasts that follow you.</h2>
              <p>Live radar, hourly updates and air quality for every city you care about.</p>
            </motion.div>
          </aside>

          {/* ---------- form pane ---------- */}
          <section className="formPane">
            <AnimatePresence mode="wait" custom={dir} initial={false}>
              {step === 'phone' && (
                <motion.div key="phone" custom={dir} variants={slide} initial="initial" animate="animate" exit="exit">
                  <motion.div variants={item} initial="hidden" animate="show">
                    <h1>Welcome back</h1>
                    <p className="sub">Log in to see today&apos;s forecast.</p>
                  </motion.div>

                  <motion.form onSubmit={send} animate={shake} className="phoneForm">
                    <label htmlFor="phone">Phone number</label>
                    <div className={`field ${error ? 'bad' : ''}`}>
                      <select aria-label="Country code" value={ci} onChange={(e) => { setCi(+e.target.value); setDigits(''); }}>
                        {COUNTRIES.map((c, i) => <option key={c.code} value={i}>{c.flag} {c.dial}</option>)}
                      </select>
                      <span className="sep" />
                      <Phone size={18} className="mut" />
                      <input
                        id="phone" inputMode="numeric" autoComplete="tel-national" placeholder={'9'.repeat(country.len).replace(/(.{5})(.*)/, '$1 $2')}
                        value={shown} onChange={(e) => { setDigits(e.target.value.replace(/\D/g, '').slice(0, country.len)); setError(''); }}
                      />
                      <AnimatePresence>
                        {valid && <motion.span className="tick" initial={{ scale: 0 }} animate={{ scale: 1 }} exit={{ scale: 0 }}><Check size={14} /></motion.span>}
                      </AnimatePresence>
                    </div>
                    <AnimatePresence>{error && <motion.p className="err" initial={{ opacity: 0, height: 0 }} animate={{ opacity: 1, height: 'auto' }} exit={{ opacity: 0, height: 0 }}>{error}</motion.p>}</AnimatePresence>

                    <motion.button type="submit" className="primaryBtn" disabled={!valid || !!busy} whileHover={valid ? { scale: 1.02 } : {}} whileTap={valid ? { scale: 0.97 } : {}}>
                      {busy === 'send' ? <i className="spinner" /> : <>Send OTP <ArrowRight size={18} /></>}
                    </motion.button>
                  </motion.form>

                  <div className="divider"><span>or</span></div>

                  <motion.button className="googleBtn" onClick={google} disabled={!!busy} whileHover={{ scale: 1.02 }} whileTap={{ scale: 0.97 }}>
                    {busy === 'google' ? <i className="spinner dark" /> : <><GoogleLogo /> Continue with Google</>}
                  </motion.button>

                  <p className="legal"><ShieldCheck size={14} /> By continuing you agree to the Terms and Privacy Policy.</p>
                </motion.div>
              )}

              {step === 'otp' && (
                <motion.div key="otp" custom={dir} variants={slide} initial="initial" animate="animate" exit="exit">
                  <button className="backBtn" onClick={back}><ArrowLeft size={16} /> Change number</button>
                  <h1>Enter the code</h1>
                  <p className="sub">We sent a 6-digit code to <b>{country.dial} {shown}</b>.</p>

                  <motion.div className="otpRow" animate={shake} onPaste={onPaste}>
                    {otp.map((v, i) => (
                      <motion.input
                        key={i} ref={(el) => { refs.current[i] = el; }} value={v} inputMode="numeric" autoComplete={i === 0 ? 'one-time-code' : 'off'}
                        maxLength={1} aria-label={`Digit ${i + 1}`} className={`${v ? 'filled' : ''} ${error ? 'bad' : ''}`} disabled={busy === 'verify'}
                        onChange={(e) => setDigit(i, e.target.value)} onKeyDown={(e) => onKey(i, e)} onFocus={(e) => e.target.select()}
                        initial={{ opacity: 0, y: 14 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.1 + i * 0.05, type: 'spring', stiffness: 200, damping: 16 }}
                      />
                    ))}
                  </motion.div>
                  <AnimatePresence>{error && <motion.p className="err" initial={{ opacity: 0, height: 0 }} animate={{ opacity: 1, height: 'auto' }} exit={{ opacity: 0, height: 0 }}>{error}</motion.p>}</AnimatePresence>

                  <motion.button className="primaryBtn" disabled={otp.some((x) => !x) || busy === 'verify'} onClick={() => verify(otp.join(''))} whileTap={{ scale: 0.97 }}>
                    {busy === 'verify' ? <i className="spinner" /> : <>Verify &amp; log in <ArrowRight size={18} /></>}
                  </motion.button>

                  <p className="resend">
                    {left > 0 ? <>Resend code in <b>0:{String(left).padStart(2, '0')}</b></> : <button onClick={send}>Resend code</button>}
                  </p>
                  <p className="hint">Demo code: {DEMO_OTP}</p>
                </motion.div>
              )}

              {step === 'done' && (
                <motion.div key="done" className="successWrap" custom={dir} variants={slide} initial="initial" animate="animate" exit="exit">
                  <motion.div className="successRing" initial={{ scale: 0 }} animate={{ scale: 1 }} transition={{ type: 'spring', stiffness: 200, damping: 14 }}>
                    <svg viewBox="0 0 52 52" width="64" height="64">
                      <motion.path d="M14 27l8 8 16-17" fill="none" stroke="#fff" strokeWidth="4" strokeLinecap="round" strokeLinejoin="round" initial={{ pathLength: 0 }} animate={{ pathLength: 1 }} transition={{ delay: 0.3, duration: 0.5 }} />
                    </svg>
                    <motion.span className="pulseRing" animate={{ scale: [1, 1.8], opacity: [0.6, 0] }} transition={{ duration: 1.4, repeat: Infinity }} />
                  </motion.div>
                  <h1>You&apos;re in</h1>
                  <p className="sub">Loading your forecast…</p>
                  <div className="bar"><motion.i initial={{ width: 0 }} animate={{ width: '100%' }} transition={{ duration: 1.5, ease: 'easeInOut' }} /></div>
                </motion.div>
              )}
            </AnimatePresence>
          </section>
        </motion.div>
      </main>
    </>
  );
}
