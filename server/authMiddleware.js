import { getOrCreateUser } from './db.js';

// Decode JWT payload without external library dependencies
function parseJwtPayload(token) {
  try {
    const parts = token.split('.');
    if (parts.length < 2) return null;
    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = Buffer.from(base64, 'base64').toString('utf8');
    return JSON.parse(jsonPayload);
  } catch (e) {
    return null;
  }
}

export async function authMiddleware(req, res, next) {
  const authHeader = req.headers.authorization;

  let userId = 'user_default';
  let email = 'student@university.edu';
  let name = 'Student';
  let photoUrl = null;

  if (authHeader && authHeader.startsWith('Bearer ')) {
    const token = authHeader.substring(7).trim();
    const payload = parseJwtPayload(token);

    if (payload) {
      userId = payload.user_id || payload.sub || payload.uid || 'usr_' + Buffer.from(payload.email || 'guest').toString('hex').slice(0, 12);
      email = payload.email || email;
      name = payload.name || payload.displayName || name;
      photoUrl = payload.picture || payload.photoURL || null;
    } else if (token.length > 3) {
      // Direct user ID / session key
      userId = 'usr_' + token.replace(/[^a-zA-Z0-9_-]/g, '').slice(0, 32);
    }
  } else if (req.headers['x-user-email']) {
    email = req.headers['x-user-email'];
    userId = 'usr_' + Buffer.from(email).toString('hex').slice(0, 16);
    name = req.headers['x-user-name'] || email.split('@')[0];
  } else {
    // Default active user session in preview
    userId = 'usr_suprakash';
    email = 'suprakashg21@gmail.com';
    name = 'Suprakash Ghosh';
  }

  try {
    const userRecord = await getOrCreateUser({ id: userId, email, name, photoUrl });
    req.user = userRecord;
    next();
  } catch (err) {
    console.error('Authentication resolution error:', err);
    res.status(500).json({ error: 'Failed to authenticate user session' });
  }
}
