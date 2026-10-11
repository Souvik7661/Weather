import React, { useState } from 'react';
import { createRoot } from 'react-dom/client';
import { AnimatePresence, MotionConfig, motion } from 'framer-motion';
import 'leaflet/dist/leaflet.css';
import App from './App.jsx';
import Login from './Login.jsx';
import './styles.css';

const KEY = 'weatherly-user';
const read = () => {
  try {
    return JSON.parse(localStorage.getItem(KEY));
  } catch {
    return null;
  }
};

function Root() {
  const [user, setUser] = useState(read);
  const onLogin = (u) => {
    try {
      localStorage.setItem(KEY, JSON.stringify(u));
    } catch {
      /* storage blocked */
    }
    setUser(u);
  };

  return (
    <AnimatePresence mode="wait">
      {user ? (
        <motion.div key="app" initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ duration: 0.5 }}>
          <App />
        </motion.div>
      ) : (
        <motion.div key="login" exit={{ opacity: 0, scale: 1.03 }} transition={{ duration: 0.4 }}>
          <Login onLogin={onLogin} />
        </motion.div>
      )}
    </AnimatePresence>
  );
}

createRoot(document.getElementById('root')).render(
  <MotionConfig reducedMotion="user">
    <Root />
  </MotionConfig>
);
