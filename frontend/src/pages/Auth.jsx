import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import api, { errMsg } from "../api";
import { useAuth } from "../auth.jsx";

export default function Auth({ mode }) {
  const isReg = mode === "register";
  const [f, setF] = useState({ name: "", email: "", password: "" });
  const [err, setErr] = useState("");
  const { login } = useAuth();
  const nav = useNavigate();
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    setErr("");
    if (isReg && f.name.trim().length < 2) return setErr("Name must be at least 2 characters");
    if (!/^\S+@\S+\.\S+$/.test(f.email)) return setErr("Enter a valid email address");
    if (f.password.length < 6) return setErr("Password must be at least 6 characters");
    try {
      const { data } = await api.post(`/auth/${isReg ? "register" : "login"}`, f);
      login(data);
      nav(data.role === "ADMIN" ? "/admin" : "/");
    } catch (x) {
      setErr(errMsg(x));
    }
  };

  return (
    <form className="card auth" onSubmit={submit} noValidate>
      <h1>{isReg ? "Create your account" : "Welcome back"}</h1>
      {err && <p className="error">{err}</p>}
      {isReg && <label>Full name<input value={f.name} onChange={set("name")} autoComplete="name" /></label>}
      <label>Email<input type="email" value={f.email} onChange={set("email")} autoComplete="email" /></label>
      <label>Password<input type="password" value={f.password} onChange={set("password")} autoComplete={isReg ? "new-password" : "current-password"} /></label>
      <button className="btn" type="submit">{isReg ? "Sign up" : "Log in"}</button>
      <p className="muted">
        {isReg ? <>Already have an account? <Link to="/login">Log in</Link></> : <>New here? <Link to="/register">Create an account</Link></>}
      </p>
      {!isReg && <p className="muted small">Admin demo: admin@cartly.com / Admin@123</p>}
    </form>
  );
}