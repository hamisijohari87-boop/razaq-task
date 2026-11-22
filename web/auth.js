import {
  GoogleAuthProvider,
  onAuthStateChanged,
  signInWithPopup,
  signOut
} from "https://www.gstatic.com/firebasejs/11.4.0/firebase-auth.js";

// Google Sign In
export async function signInWithGoogle(auth) {
  const provider = new GoogleAuthProvider();
  return await signInWithPopup(auth, provider);
}

// Kusikiliza mabadiliko ya akaunti
export function watchAuthState(auth, callback) {
  return onAuthStateChanged(auth, callback);
}

// Kutoka kwenye akaunti
export async function signOutUser(auth) {
  return await signOut(auth);
}
