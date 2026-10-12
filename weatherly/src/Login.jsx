import { useEffect, useRef, useState } from 'react';
import { AnimatePresence, motion, useAnimationControls, useMotionValue, useSpring, useTransform } from 'framer-motion';
import { ArrowLeft, ArrowRight, Check, ChevronDown, Phone, ShieldCheck } from 'lucide-react';
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
  { code: 'AF', flag: '🇦🇫', dial: '+93', len: 9 },
  { code: 'AL', flag: '🇦🇱', dial: '+355', len: 9 },
  { code: 'DZ', flag: '🇩🇿', dial: '+213', len: 9 },
  { code: 'AD', flag: '🇦🇩', dial: '+376', len: 6 },
  { code: 'AO', flag: '🇦🇴', dial: '+244', len: 9 },
  { code: 'AG', flag: '🇦🇬', dial: '+1268', len: 10 },
  { code: 'AR', flag: '🇦🇷', dial: '+54', len: 10 },
  { code: 'AM', flag: '🇦🇲', dial: '+374', len: 8 },
  { code: 'AU', flag: '🇦🇺', dial: '+61', len: 9 },
  { code: 'AT', flag: '🇦🇹', dial: '+43', len: 10 },
  { code: 'AZ', flag: '🇦🇿', dial: '+994', len: 9 },
  { code: 'BS', flag: '🇧🇸', dial: '+1242', len: 10 },
  { code: 'BH', flag: '🇧🇭', dial: '+973', len: 8 },
  { code: 'BD', flag: '🇧🇩', dial: '+880', len: 10 },
  { code: 'BB', flag: '🇧🇧', dial: '+1246', len: 10 },
  { code: 'BY', flag: '🇧🇾', dial: '+375', len: 9 },
  { code: 'BE', flag: '🇧🇪', dial: '+32', len: 9 },
  { code: 'BZ', flag: '🇧🇿', dial: '+501', len: 7 },
  { code: 'BJ', flag: '🇧🇯', dial: '+229', len: 10 },
  { code: 'BT', flag: '🇧🇹', dial: '+975', len: 8 },
  { code: 'BO', flag: '🇧🇴', dial: '+591', len: 8 },
  { code: 'BA', flag: '🇧🇦', dial: '+387', len: 8 },
  { code: 'BW', flag: '🇧🇼', dial: '+267', len: 8 },
  { code: 'BR', flag: '🇧🇷', dial: '+55', len: 11 },
  { code: 'BN', flag: '🇧🇳', dial: '+673', len: 7 },
  { code: 'BG', flag: '🇧🇬', dial: '+359', len: 9 },
  { code: 'BF', flag: '🇧🇫', dial: '+226', len: 8 },
  { code: 'BI', flag: '🇧🇮', dial: '+257', len: 8 },
  { code: 'CV', flag: '🇨🇻', dial: '+238', len: 7 },
  { code: 'KH', flag: '🇰🇭', dial: '+855', len: 9 },
  { code: 'CM', flag: '🇨🇲', dial: '+237', len: 9 },
  { code: 'CA', flag: '🇨🇦', dial: '+1', len: 10 },
  { code: 'CF', flag: '🇨🇫', dial: '+236', len: 8 },
  { code: 'TD', flag: '🇹🇩', dial: '+235', len: 8 },
  { code: 'CL', flag: '🇨🇱', dial: '+56', len: 9 },
  { code: 'CN', flag: '🇨🇳', dial: '+86', len: 11 },
  { code: 'CO', flag: '🇨🇴', dial: '+57', len: 10 },
  { code: 'KM', flag: '🇰🇲', dial: '+269', len: 7 },
  { code: 'CG', flag: '🇨🇬', dial: '+242', len: 9 },
  { code: 'CD', flag: '🇨🇩', dial: '+243', len: 9 },
  { code: 'CR', flag: '🇨🇷', dial: '+506', len: 8 },
  { code: 'HR', flag: '🇭🇷', dial: '+385', len: 9 },
  { code: 'CU', flag: '🇨🇺', dial: '+53', len: 8 },
  { code: 'CY', flag: '🇨🇾', dial: '+357', len: 8 },
  { code: 'CZ', flag: '🇨🇿', dial: '+420', len: 9 },
  { code: 'DK', flag: '🇩🇰', dial: '+45', len: 8 },
  { code: 'DJ', flag: '🇩🇯', dial: '+253', len: 8 },
  { code: 'DM', flag: '🇩🇲', dial: '+1767', len: 10 },
  { code: 'DO', flag: '🇩🇴', dial: '+1809', len: 10 },
  { code: 'EC', flag: '🇪🇨', dial: '+593', len: 9 },
  { code: 'EG', flag: '🇪🇬', dial: '+20', len: 10 },
  { code: 'SV', flag: '🇸🇻', dial: '+503', len: 8 },
  { code: 'GQ', flag: '🇬🇶', dial: '+240', len: 9 },
  { code: 'ER', flag: '🇪🇷', dial: '+291', len: 7 },
  { code: 'EE', flag: '🇪🇪', dial: '+372', len: 8 },
  { code: 'SZ', flag: '🇸🇿', dial: '+268', len: 8 },
  { code: 'ET', flag: '🇪🇹', dial: '+251', len: 9 },
  { code: 'FJ', flag: '🇫🇯', dial: '+679', len: 7 },
  { code: 'FI', flag: '🇫🇮', dial: '+358', len: 10 },
  { code: 'FR', flag: '🇫🇷', dial: '+33', len: 9 },
  { code: 'GA', flag: '🇬🇦', dial: '+241', len: 8 },
  { code: 'GM', flag: '🇬🇲', dial: '+220', len: 7 },
  { code: 'GE', flag: '🇬🇪', dial: '+995', len: 9 },
  { code: 'DE', flag: '🇩🇪', dial: '+49', len: 11 },
  { code: 'GH', flag: '🇬🇭', dial: '+233', len: 9 },
  { code: 'GR', flag: '🇬🇷', dial: '+30', len: 10 },
  { code: 'GD', flag: '🇬🇩', dial: '+1473', len: 10 },
  { code: 'GT', flag: '🇬🇹', dial: '+502', len: 8 },
  { code: 'GN', flag: '🇬🇳', dial: '+224', len: 9 },
  { code: 'GW', flag: '🇬🇼', dial: '+245', len: 9 },
  { code: 'GY', flag: '🇬🇾', dial: '+592', len: 7 },
  { code: 'HT', flag: '🇭🇹', dial: '+509', len: 8 },
  { code: 'HN', flag: '🇭🇳', dial: '+504', len: 8 },
  { code: 'HU', flag: '🇭🇺', dial: '+36', len: 9 },
  { code: 'IS', flag: '🇮🇸', dial: '+354', len: 7 },
  { code: 'IN', flag: '🇮🇳', dial: '+91', len: 10 },
  { code: 'ID', flag: '🇮🇩', dial: '+62', len: 11 },
  { code: 'IR', flag: '🇮🇷', dial: '+98', len: 10 },
  { code: 'IQ', flag: '🇮🇶', dial: '+964', len: 10 },
  { code: 'IE', flag: '🇮🇪', dial: '+353', len: 9 },
  { code: 'IL', flag: '🇮🇱', dial: '+972', len: 9 },
  { code: 'IT', flag: '🇮🇹', dial: '+39', len: 10 },
  { code: 'JM', flag: '🇯🇲', dial: '+1876', len: 10 },
  { code: 'JP', flag: '🇯🇵', dial: '+81', len: 10 },
  { code: 'JO', flag: '🇯🇴', dial: '+962', len: 9 },
  { code: 'KZ', flag: '🇰🇿', dial: '+7', len: 10 },
  { code: 'KE', flag: '🇰🇪', dial: '+254', len: 9 },
  { code: 'KI', flag: '🇰🇮', dial: '+686', len: 8 },
  { code: 'KP', flag: '🇰🇵', dial: '+850', len: 10 },
  { code: 'KR', flag: '🇰🇷', dial: '+82', len: 10 },
  { code: 'KW', flag: '🇰🇼', dial: '+965', len: 8 },
  { code: 'KG', flag: '🇰🇬', dial: '+996', len: 9 },
  { code: 'LA', flag: '🇱🇦', dial: '+856', len: 10 },
  { code: 'LV', flag: '🇱🇻', dial: '+371', len: 8 },
  { code: 'LB', flag: '🇱🇧', dial: '+961', len: 8 },
  { code: 'LS', flag: '🇱🇸', dial: '+266', len: 8 },
  { code: 'LR', flag: '🇱🇷', dial: '+231', len: 8 },
  { code: 'LY', flag: '🇱🇾', dial: '+218', len: 9 },
  { code: 'LI', flag: '🇱🇮', dial: '+423', len: 7 },
  { code: 'LT', flag: '🇱🇹', dial: '+370', len: 8 },
  { code: 'LU', flag: '🇱🇺', dial: '+352', len: 9 },
  { code: 'MG', flag: '🇲🇬', dial: '+261', len: 9 },
  { code: 'MW', flag: '🇲🇼', dial: '+265', len: 9 },
  { code: 'MY', flag: '🇲🇾', dial: '+60', len: 9 },
  { code: 'MV', flag: '🇲🇻', dial: '+960', len: 7 },
  { code: 'ML', flag: '🇲🇱', dial: '+223', len: 8 },
  { code: 'MT', flag: '🇲🇹', dial: '+356', len: 8 },
  { code: 'MH', flag: '🇲🇭', dial: '+692', len: 7 },
  { code: 'MR', flag: '🇲🇷', dial: '+222', len: 8 },
  { code: 'MU', flag: '🇲🇺', dial: '+230', len: 8 },
  { code: 'MX', flag: '🇲🇽', dial: '+52', len: 10 },
  { code: 'FM', flag: '🇫🇲', dial: '+691', len: 7 },
  { code: 'MD', flag: '🇲🇩', dial: '+373', len: 8 },
  { code: 'MC', flag: '🇲🇨', dial: '+377', len: 9 },
  { code: 'MN', flag: '🇲🇳', dial: '+976', len: 8 },
  { code: 'ME', flag: '🇲🇪', dial: '+382', len: 8 },
  { code: 'MA', flag: '🇲🇦', dial: '+212', len: 9 },
  { code: 'MZ', flag: '🇲🇿', dial: '+258', len: 9 },
  { code: 'MM', flag: '🇲🇲', dial: '+95', len: 10 },
  { code: 'NA', flag: '🇳🇦', dial: '+264', len: 9 },
  { code: 'NR', flag: '🇳🇷', dial: '+674', len: 7 },
  { code: 'NP', flag: '🇳🇵', dial: '+977', len: 10 },
  { code: 'NL', flag: '🇳🇱', dial: '+31', len: 9 },
  { code: 'NZ', flag: '🇳🇿', dial: '+64', len: 9 },
  { code: 'NI', flag: '🇳🇮', dial: '+505', len: 8 },
  { code: 'NE', flag: '🇳🇪', dial: '+227', len: 8 },
  { code: 'NG', flag: '🇳🇬', dial: '+234', len: 10 },
  { code: 'MK', flag: '🇲🇰', dial: '+389', len: 8 },
  { code: 'NO', flag: '🇳🇴', dial: '+47', len: 8 },
  { code: 'OM', flag: '🇴🇲', dial: '+968', len: 8 },
  { code: 'PK', flag: '🇵🇰', dial: '+92', len: 10 },
  { code: 'PW', flag: '🇵🇼', dial: '+680', len: 7 },
  { code: 'PS', flag: '🇵🇸', dial: '+970', len: 9 },
  { code: 'PA', flag: '🇵🇦', dial: '+507', len: 8 },
  { code: 'PG', flag: '🇵🇬', dial: '+675', len: 8 },
  { code: 'PY', flag: '🇵🇾', dial: '+595', len: 9 },
  { code: 'PE', flag: '🇵🇪', dial: '+51', len: 9 },
  { code: 'PH', flag: '🇵🇭', dial: '+63', len: 10 },
  { code: 'PL', flag: '🇵🇱', dial: '+48', len: 9 },
  { code: 'PT', flag: '🇵🇹', dial: '+351', len: 9 },
  { code: 'QA', flag: '🇶🇦', dial: '+974', len: 8 },
  { code: 'RO', flag: '🇷🇴', dial: '+40', len: 9 },
  { code: 'RU', flag: '🇷🇺', dial: '+7', len: 10 },
  { code: 'RW', flag: '🇷🇼', dial: '+250', len: 9 },
  { code: 'KN', flag: '🇰🇳', dial: '+1869', len: 10 },
  { code: 'LC', flag: '🇱🇨', dial: '+1758', len: 10 },
  { code: 'VC', flag: '🇻🇨', dial: '+1784', len: 10 },
  { code: 'WS', flag: '🇼🇸', dial: '+685', len: 7 },
  { code: 'SM', flag: '🇸🇲', dial: '+378', len: 10 },
  { code: 'ST', flag: '🇸🇹', dial: '+239', len: 7 },
  { code: 'SA', flag: '🇸🇦', dial: '+966', len: 9 },
  { code: 'SN', flag: '🇸🇳', dial: '+221', len: 9 },
  { code: 'RS', flag: '🇷🇸', dial: '+381', len: 9 },
  { code: 'SC', flag: '🇸🇨', dial: '+248', len: 7 },
  { code: 'SL', flag: '🇸🇱', dial: '+232', len: 8 },
  { code: 'SG', flag: '🇸🇬', dial: '+65', len: 8 },
  { code: 'SK', flag: '🇸🇰', dial: '+421', len: 9 },
  { code: 'SI', flag: '🇸🇮', dial: '+386', len: 8 },
  { code: 'SB', flag: '🇸🇧', dial: '+677', len: 7 },
  { code: 'SO', flag: '🇸🇴', dial: '+252', len: 8 },
  { code: 'ZA', flag: '🇿🇦', dial: '+27', len: 9 },
  { code: 'SS', flag: '🇸🇸', dial: '+211', len: 9 },
  { code: 'ES', flag: '🇪🇸', dial: '+34', len: 9 },
  { code: 'LK', flag: '🇱🇰', dial: '+94', len: 9 },
  { code: 'SD', flag: '🇸🇩', dial: '+249', len: 9 },
  { code: 'SR', flag: '🇸🇷', dial: '+597', len: 7 },
  { code: 'SE', flag: '🇸🇪', dial: '+46', len: 9 },
  { code: 'CH', flag: '🇨🇭', dial: '+41', len: 9 },
  { code: 'SY', flag: '🇸🇾', dial: '+963', len: 9 },
  { code: 'TW', flag: '🇹🇼', dial: '+886', len: 9 },
  { code: 'TJ', flag: '🇹🇯', dial: '+992', len: 9 },
  { code: 'TZ', flag: '🇹🇿', dial: '+255', len: 9 },
  { code: 'TH', flag: '🇹🇭', dial: '+66', len: 9 },
  { code: 'TL', flag: '🇹🇱', dial: '+670', len: 8 },
  { code: 'TG', flag: '🇹🇬', dial: '+228', len: 8 },
  { code: 'TO', flag: '🇹🇴', dial: '+676', len: 7 },
  { code: 'TT', flag: '🇹🇹', dial: '+1868', len: 10 },
  { code: 'TN', flag: '🇹🇳', dial: '+216', len: 8 },
  { code: 'TR', flag: '🇹🇷', dial: '+90', len: 10 },
  { code: 'TM', flag: '🇹🇲', dial: '+993', len: 8 },
  { code: 'TV', flag: '🇹🇻', dial: '+688', len: 6 },
  { code: 'UG', flag: '🇺🇬', dial: '+256', len: 9 },
  { code: 'UA', flag: '🇺🇦', dial: '+380', len: 9 },
  { code: 'AE', flag: '🇦🇪', dial: '+971', len: 9 },
  { code: 'GB', flag: '🇬🇧', dial: '+44', len: 10 },
  { code: 'US', flag: '🇺🇸', dial: '+1', len: 10 },
  { code: 'UY', flag: '🇺🇾', dial: '+598', len: 8 },
  { code: 'UZ', flag: '🇺🇿', dial: '+998', len: 9 },
  { code: 'VU', flag: '🇻🇺', dial: '+678', len: 7 },
  { code: 'VA', flag: '🇻🇦', dial: '+379', len: 10 },
  { code: 'VE', flag: '🇻🇪', dial: '+58', len: 10 },
  { code: 'VN', flag: '🇻🇳', dial: '+84', len: 10 },
  { code: 'YE', flag: '🇾🇪', dial: '+967', len: 9 },
  { code: 'ZM', flag: '🇿🇲', dial: '+260', len: 9 },
  { code: 'ZW', flag: '🇿🇼', dial: '+263', len: 9 },
  { code: 'AS', flag: '🇦🇸', dial: '+1684', len: 10 },
  { code: 'AI', flag: '🇦🇮', dial: '+1264', len: 10 },
  { code: 'AW', flag: '🇦🇼', dial: '+297', len: 7 },
  { code: 'BM', flag: '🇧🇲', dial: '+1441', len: 10 },
  { code: 'BQ', flag: '🇧🇶', dial: '+599', len: 7 },
  { code: 'IO', flag: '🇮🇴', dial: '+246', len: 7 },
  { code: 'VG', flag: '🇻🇬', dial: '+1284', len: 10 },
  { code: 'KY', flag: '🇰🇾', dial: '+1345', len: 10 },
  { code: 'CW', flag: '🇨🇼', dial: '+599', len: 8 },
  { code: 'CX', flag: '🇨🇽', dial: '+61', len: 9 },
  { code: 'CC', flag: '🇨🇨', dial: '+61', len: 9 },
  { code: 'CK', flag: '🇨🇰', dial: '+682', len: 5 },
  { code: 'FK', flag: '🇫🇰', dial: '+500', len: 5 },
  { code: 'FO', flag: '🇫🇴', dial: '+298', len: 6 },
  { code: 'GF', flag: '🇬🇫', dial: '+594', len: 9 },
  { code: 'PF', flag: '🇵🇫', dial: '+689', len: 8 },
  { code: 'GI', flag: '🇬🇮', dial: '+350', len: 8 },
  { code: 'GL', flag: '🇬🇱', dial: '+299', len: 6 },
  { code: 'GP', flag: '🇬🇵', dial: '+590', len: 9 },
  { code: 'GU', flag: '🇬🇺', dial: '+1671', len: 10 },
  { code: 'GG', flag: '🇬🇬', dial: '+44', len: 10 },
  { code: 'HK', flag: '🇭🇰', dial: '+852', len: 8 },
  { code: 'IM', flag: '🇮🇲', dial: '+44', len: 10 },
  { code: 'JE', flag: '🇯🇪', dial: '+44', len: 10 },
  { code: 'MO', flag: '🇲🇴', dial: '+853', len: 8 },
  { code: 'MQ', flag: '🇲🇶', dial: '+596', len: 9 },
  { code: 'YT', flag: '🇾🇹', dial: '+262', len: 9 },
  { code: 'MS', flag: '🇲🇸', dial: '+1664', len: 10 },
  { code: 'NC', flag: '🇳🇨', dial: '+687', len: 6 },
  { code: 'NU', flag: '🇳🇺', dial: '+683', len: 4 },
  { code: 'NF', flag: '🇳🇫', dial: '+672', len: 6 },
  { code: 'MP', flag: '🇲🇵', dial: '+1670', len: 10 },
  { code: 'PR', flag: '🇵🇷', dial: '+1787', len: 10 },
  { code: 'RE', flag: '🇷🇪', dial: '+262', len: 9 },
  { code: 'BL', flag: '🇧🇱', dial: '+590', len: 9 },
  { code: 'SH', flag: '🇸🇭', dial: '+290', len: 4 },
  { code: 'MF', flag: '🇲🇫', dial: '+590', len: 9 },
  { code: 'PM', flag: '🇵🇲', dial: '+508', len: 6 },
  { code: 'SX', flag: '🇸🇽', dial: '+1721', len: 10 },
  { code: 'SJ', flag: '🇸🇯', dial: '+47', len: 8 },
  { code: 'TK', flag: '🇹🇰', dial: '+690', len: 4 },
  { code: 'TC', flag: '🇹🇨', dial: '+1649', len: 10 },
  { code: 'VI', flag: '🇻🇮', dial: '+1340', len: 10 },
  { code: 'WF', flag: '🇼🇫', dial: '+681', len: 6 },
  { code: 'EH', flag: '🇪🇭', dial: '+212', len: 9 },
  { code: 'AX', flag: '🇦🇽', dial: '+358', len: 10 }
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
  hidden: { opacity: 0, y: 35, scale: 0.97 },
  show: { opacity: 1, y: 0, scale: 1, transition: { type: 'spring', stiffness: 95, damping: 18, staggerChildren: 0.08, delayChildren: 0.12 } },
};
const item = { hidden: { opacity: 0, y: 14 }, show: { opacity: 1, y: 0, transition: { type: 'spring', stiffness: 140, damping: 18 } } };
const slide = {
  initial: (d) => ({ opacity: 0, x: 35 * d }),
  animate: { opacity: 1, x: 0, transition: { type: 'spring', stiffness: 160, damping: 20 } },
  exit: (d) => ({ opacity: 0, x: -35 * d, transition: { duration: 0.18 } }),
};

export default function Login({ onLogin }) {
  const [step, setStep] = useState('phone'); // phone | otp | done
  const [dir, setDir] = useState(1);
  const defaultCi = Math.max(0, COUNTRIES.findIndex((c) => c.code === 'IN'));
  const [ci, setCi] = useState(defaultCi >= 0 ? defaultCi : 0);
  const [digits, setDigits] = useState('');
  const [otp, setOtp] = useState(['', '', '', '', '', '']);
  const [busy, setBusy] = useState(null); // send | verify | google
  const [error, setError] = useState('');
  const [left, setLeft] = useState(0);
  const [user, setUser] = useState(null);
  const refs = useRef([]);
  const shake = useAnimationControls();
  const country = COUNTRIES[ci] || COUNTRIES[0];
  const valid = digits.length === country.len;
  const shown = country.len === 10 && digits.length > 5 ? `${digits.slice(0, 5)} ${digits.slice(5)}` : digits;

  /* 3D Interactive Mouse Tilt */
  const mouseX = useMotionValue(0);
  const mouseY = useMotionValue(0);
  const smoothX = useSpring(mouseX, { stiffness: 45, damping: 20 });
  const smoothY = useSpring(mouseY, { stiffness: 45, damping: 20 });
  const cardRotateX = useTransform(smoothY, [-0.5, 0.5], [4, -4]);
  const cardRotateY = useTransform(smoothX, [-0.5, 0.5], [-5, 5]);

  useEffect(() => {
    const handleMove = (e) => {
      mouseX.set(e.clientX / window.innerWidth - 0.5);
      mouseY.set(e.clientY / window.innerHeight - 0.5);
    };
    window.addEventListener('pointermove', handleMove);
    return () => window.removeEventListener('pointermove', handleMove);
  }, [mouseX, mouseY]);

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

  const fail = (msg) => { setError(msg); shake.start({ x: [0, -12, 12, -8, 8, -4, 4, 0], transition: { duration: 0.45 } }); };

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
      <div className="loginFloorReflection" />

      <main className="loginShell">
        <motion.div
          className="loginCard"
          variants={card}
          initial="hidden"
          animate="show"
          style={{ rotateX: cardRotateX, rotateY: cardRotateY }}
        >
          {/* ---------- brand pane (left) ---------- */}
          <aside className="brandPane">
            {/* Logo */}
            <motion.div variants={item} className="brandLogo">
              <motion.img
                src="/assets/logo_cloud_3d.png"
                alt="Weatherly Cloud"
                className="brandLogoCloud"
                animate={{ y: [0, -3.5, 0] }}
                transition={{ duration: 4, repeat: Infinity, ease: 'easeInOut' }}
              />
              <div className="brandLogoText">
                <b>Weatherly</b>
                <small>See Beyond the Sky</small>
              </div>
            </motion.div>

            {/* Center 3D Weather Orb with Concentric Orbital Rings */}
            <motion.div variants={item} className="brandOrbCenter">
              {/* Concentric Glowing Orbital Rings */}
              <svg className="orbitalRings" viewBox="0 0 320 320" fill="none">
                <circle cx="160" cy="160" r="132" stroke="rgba(255, 255, 255, 0.16)" strokeWidth="1" strokeDasharray="5 7" />
                <circle cx="160" cy="160" r="132" stroke="url(#orbitGradOuter)" strokeWidth="1.6" strokeDasharray="110 320" strokeLinecap="round" />
                
                <circle cx="160" cy="160" r="102" stroke="rgba(255, 255, 255, 0.22)" strokeWidth="1" strokeDasharray="3 5" />
                <circle cx="160" cy="160" r="102" stroke="url(#orbitGradInner)" strokeWidth="1.6" strokeDasharray="75 280" strokeDashoffset="-35" strokeLinecap="round" />
                
                <defs>
                  <linearGradient id="orbitGradOuter" x1="0%" y1="0%" x2="100%" y2="100%">
                    <stop offset="0%" stopColor="#ffffff" stopOpacity="0.85" />
                    <stop offset="50%" stopColor="#9ec5fe" stopOpacity="0.5" />
                    <stop offset="100%" stopColor="#3b82f6" stopOpacity="0" />
                  </linearGradient>
                  <linearGradient id="orbitGradInner" x1="100%" y1="0%" x2="0%" y2="100%">
                    <stop offset="0%" stopColor="#ffffff" stopOpacity="0.9" />
                    <stop offset="60%" stopColor="#ffb86c" stopOpacity="0.6" />
                    <stop offset="100%" stopColor="#ff9a3c" stopOpacity="0" />
                  </linearGradient>
                </defs>
              </svg>

              {/* Floating Mini Cloud on Outer Orbital Track */}
              <motion.img
                src="/assets/mini_cloud_3d.png"
                alt="Mini Cloud"
                className="miniCloudTop"
                animate={{ y: [0, -5, 0], x: [0, 2, 0] }}
                transition={{ duration: 4.8, repeat: Infinity, ease: 'easeInOut' }}
              />

              {/* Golden Sun Corona Halo Glow */}
              <motion.div
                className="orbHaloGlow"
                animate={{ scale: [0.95, 1.1, 0.95], opacity: [0.38, 0.58, 0.38] }}
                transition={{ duration: 4.5, repeat: Infinity, ease: 'easeInOut' }}
              />

              {/* 3D Sun & Cloud Floating Orb */}
              <motion.img
                src="/assets/sun_cloud_3d.png"
                alt="3D Weather Orb"
                className="brandOrbImg"
                animate={{
                  y: [0, -8, 0],
                  scale: [1, 1.018, 1],
                }}
                transition={{ duration: 5.5, repeat: Infinity, ease: 'easeInOut' }}
              />

              {/* Top-Right Humidity Glass Chip */}
              <motion.div
                className="glassChip chipHumidity"
                animate={{ y: [0, -6, 0] }}
                transition={{ duration: 4.8, repeat: Infinity, ease: 'easeInOut', delay: 0.3 }}
                whileHover={{ scale: 1.06, y: -4, transition: { type: 'spring', stiffness: 400, damping: 15 } }}
                whileTap={{ scale: 0.96 }}
              >
                <img src="/assets/chip_droplet_3d.png" alt="Humidity" className="chip3dIcon droplet" />
                <div className="chipMeta">
                  <span className="chipTitle">Humidity</span>
                  <b className="chipValue">78%</b>
                </div>
              </motion.div>

              {/* Bottom-Left AQI Glass Chip */}
              <motion.div
                className="glassChip chipAqi"
                animate={{ y: [0, -7, 0] }}
                transition={{ duration: 5.2, repeat: Infinity, ease: 'easeInOut', delay: 1.1 }}
                whileHover={{ scale: 1.06, y: -4, transition: { type: 'spring', stiffness: 400, damping: 15 } }}
                whileTap={{ scale: 0.96 }}
              >
                <img src="/assets/chip_leaf_3d.png" alt="AQI" className="chip3dIcon leaf" />
                <div className="chipMeta">
                  <b className="chipValue">AQI 78</b>
                  <span className="chipTitle good">Good</span>
                </div>
              </motion.div>
            </motion.div>

            {/* Bottom Headline & Pagination Indicators */}
            <div className="brandBottom">
              <motion.h2 variants={item} className="brandHeadline">
                Forecasts that <span className="brandAccent">follow you.</span>
              </motion.h2>
              <motion.p variants={item} className="brandDesc">
                Live radar, hourly updates and air quality for every city you care about.
              </motion.p>
              <motion.div variants={item} className="brandPills">
                <motion.span
                  className="pill active"
                  animate={{
                    boxShadow: [
                      '0 0 8px rgba(59, 130, 246, 0.6)',
                      '0 0 16px rgba(59, 130, 246, 0.95)',
                      '0 0 8px rgba(59, 130, 246, 0.6)',
                    ],
                  }}
                  transition={{ duration: 2.5, repeat: Infinity, ease: 'easeInOut' }}
                />
                <span className="pill" />
                <span className="pill" />
                <span className="pill" />
              </motion.div>
            </div>
          </aside>

          {/* ---------- form pane (right) ---------- */}
          <section className="formPane">
            <AnimatePresence mode="wait" custom={dir} initial={false}>
              {step === 'phone' && (
                <motion.div key="phone" custom={dir} variants={slide} initial="initial" animate="animate" exit="exit" className="formContent">
                  <motion.div variants={item} initial="hidden" animate="show" className="formHeader">
                    <h1>Welcome back</h1>
                    <p className="formSub">Log in to see today&apos;s forecast.</p>
                  </motion.div>

                  <motion.form onSubmit={send} animate={shake} className="phoneForm">
                    <label htmlFor="phone" className="fieldLabel">Phone number</label>
                    <div className={`unifiedField ${error ? 'bad' : ''}`}>
                      <div className="countryPickerWrap">
                        <span className="countryFlag">{country.flag}</span>
                        <span className="countryDial">{country.dial}</span>
                        <ChevronDown size={14} className="chevron" />
                        <select
                          aria-label="Country code"
                          value={ci}
                          onChange={(e) => {
                            setCi(+e.target.value);
                            setDigits('');
                          }}
                          className="countrySelect"
                        >
                          {COUNTRIES.map((c, i) => (
                            <option key={c.code} value={i}>
                              {c.flag} {c.dial} ({c.code})
                            </option>
                          ))}
                        </select>
                      </div>

                      <span className="fieldSep" />

                      <div className="phoneInputWrap">
                        <Phone size={17} className="phoneIcon" />
                        <input
                          id="phone"
                          inputMode="numeric"
                          autoComplete="tel-national"
                          placeholder={country.len === 10 ? '98765 43210' : '9'.repeat(country.len)}
                          value={shown}
                          onChange={(e) => {
                            setDigits(e.target.value.replace(/\D/g, '').slice(0, country.len));
                            setError('');
                          }}
                        />
                      </div>

                      <AnimatePresence>
                        {valid && (
                          <motion.span
                            className="greenTick"
                            initial={{ scale: 0, opacity: 0, rotate: -45 }}
                            animate={{ scale: 1, opacity: 1, rotate: 0 }}
                            exit={{ scale: 0, opacity: 0, rotate: 45 }}
                            transition={{ type: 'spring', stiffness: 450, damping: 18 }}
                          >
                            <Check size={13} strokeWidth={3} />
                          </motion.span>
                        )}
                      </AnimatePresence>
                    </div>

                    <AnimatePresence>
                      {error && (
                        <motion.p
                          className="err"
                          initial={{ opacity: 0, height: 0 }}
                          animate={{ opacity: 1, height: 'auto' }}
                          exit={{ opacity: 0, height: 0 }}
                        >
                          {error}
                        </motion.p>
                      )}
                    </AnimatePresence>

                    <motion.button
                      type="submit"
                      className="sendOtpBtn"
                      disabled={!valid || !!busy}
                      whileHover={valid ? { scale: 1.02, boxShadow: '0 14px 34px rgba(43, 107, 245, 0.58)' } : {}}
                      whileTap={valid ? { scale: 0.975 } : {}}
                    >
                      {busy === 'send' ? (
                        <i className="spinner" />
                      ) : (
                        <>
                          <span>Send OTP</span>
                          <ArrowRight size={18} />
                        </>
                      )}
                    </motion.button>
                  </motion.form>

                  <div className="orDivider">
                    <span>or</span>
                  </div>

                  <motion.button
                    className="googleAuthBtn"
                    onClick={google}
                    disabled={!!busy}
                    whileHover={{ scale: 1.02, y: -1, boxShadow: '0 10px 28px rgba(0, 0, 0, 0.35)' }}
                    whileTap={{ scale: 0.98 }}
                  >
                    {busy === 'google' ? (
                      <i className="spinner dark" />
                    ) : (
                      <>
                        <GoogleLogo />
                        <span>Continue with Google</span>
                      </>
                    )}
                  </motion.button>

                  <div className="legalTerms">
                    <ShieldCheck size={15} className="shieldIcon" />
                    <span>
                      By continuing you agree to the <a href="#terms">Terms</a> and <a href="#privacy">Privacy Policy</a>.
                    </span>
                  </div>
                </motion.div>
              )}

              {step === 'otp' && (
                <motion.div key="otp" custom={dir} variants={slide} initial="initial" animate="animate" exit="exit" className="formContent">
                  <motion.button
                    className="backBtn"
                    onClick={back}
                    whileHover={{ scale: 1.04, x: -2 }}
                    whileTap={{ scale: 0.96 }}
                  >
                    <ArrowLeft size={16} /> Change number
                  </motion.button>
                  <div className="formHeader">
                    <h1>Enter the code</h1>
                    <p className="formSub">We sent a 6-digit code to <b>{country.dial} {shown}</b>.</p>
                  </div>

                  <motion.div className="otpRow" animate={shake} onPaste={onPaste}>
                    {otp.map((v, i) => (
                      <motion.input
                        key={i}
                        ref={(el) => { refs.current[i] = el; }}
                        value={v}
                        inputMode="numeric"
                        autoComplete={i === 0 ? 'one-time-code' : 'off'}
                        maxLength={1}
                        aria-label={`Digit ${i + 1}`}
                        className={`${v ? 'filled' : ''} ${error ? 'bad' : ''}`}
                        disabled={busy === 'verify'}
                        onChange={(e) => setDigit(i, e.target.value)}
                        onKeyDown={(e) => onKey(i, e)}
                        onFocus={(e) => e.target.select()}
                        initial={{ opacity: 0, y: 16 }}
                        animate={{ opacity: 1, y: 0 }}
                        whileFocus={{ scale: 1.06 }}
                        transition={{ delay: 0.08 + i * 0.04, type: 'spring', stiffness: 240, damping: 18 }}
                      />
                    ))}
                  </motion.div>

                  <AnimatePresence>
                    {error && (
                      <motion.p
                        className="err"
                        initial={{ opacity: 0, height: 0 }}
                        animate={{ opacity: 1, height: 'auto' }}
                        exit={{ opacity: 0, height: 0 }}
                      >
                        {error}
                      </motion.p>
                    )}
                  </AnimatePresence>

                  <motion.button
                    className="sendOtpBtn"
                    disabled={otp.some((x) => !x) || busy === 'verify'}
                    onClick={() => verify(otp.join(''))}
                    whileHover={!otp.some((x) => !x) ? { scale: 1.02, boxShadow: '0 14px 34px rgba(43, 107, 245, 0.58)' } : {}}
                    whileTap={{ scale: 0.975 }}
                  >
                    {busy === 'verify' ? <i className="spinner" /> : <><span>Verify &amp; log in</span> <ArrowRight size={18} /></>}
                  </motion.button>

                  <p className="resend">
                    {left > 0 ? <>Resend code in <b>0:{String(left).padStart(2, '0')}</b></> : <button onClick={send}>Resend code</button>}
                  </p>
                  <p className="hint">Demo code: {DEMO_OTP}</p>
                </motion.div>
              )}

              {step === 'done' && (
                <motion.div key="done" className="successWrap" custom={dir} variants={slide} initial="initial" animate="animate" exit="exit">
                  <motion.div className="successRing" initial={{ scale: 0 }} animate={{ scale: 1 }} transition={{ type: 'spring', stiffness: 220, damping: 14 }}>
                    <svg viewBox="0 0 52 52" width="60" height="60">
                      <motion.path
                        d="M14 27l8 8 16-17"
                        fill="none"
                        stroke="#fff"
                        strokeWidth="4"
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        initial={{ pathLength: 0 }}
                        animate={{ pathLength: 1 }}
                        transition={{ delay: 0.22, duration: 0.55, ease: 'easeOut' }}
                      />
                    </svg>
                    <motion.span className="pulseRing" animate={{ scale: [1, 1.8], opacity: [0.7, 0] }} transition={{ duration: 1.5, repeat: Infinity }} />
                    <motion.span className="pulseRing2" animate={{ scale: [1, 2.2], opacity: [0.5, 0] }} transition={{ duration: 1.5, repeat: Infinity, delay: 0.4 }} />
                  </motion.div>
                  <h1>You&apos;re in</h1>
                  <p className="sub">Loading your forecast…</p>
                  <div className="bar">
                    <motion.i initial={{ width: 0 }} animate={{ width: '100%' }} transition={{ duration: 1.5, ease: 'easeInOut' }} />
                  </div>
                </motion.div>
              )}
            </AnimatePresence>
          </section>
        </motion.div>
      </main>
    </>
  );
}