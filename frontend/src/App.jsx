import { lazy, Suspense } from "react";
import { Routes, Route, NavLink, Navigate, useNavigate } from "react-router-dom";
import { useAuth } from "./auth.jsx";
import { useCart } from "./cart.jsx";
import Shop from "./pages/Shop.jsx";
import Auth from "./pages/Auth.jsx";

// Code splitting: these load only when visited
const Cart = lazy(() => import("./pages/Cart.jsx"));
const Orders = lazy(() => import("./pages/Orders.jsx"));
const Admin = lazy(() => import("./pages/Admin.jsx"));

function Protected({ role, children }) {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  if (role && user.role !== role) return <Navigate to="/" replace />;
  return children;
}

function Navbar() {
  const { user, logout } = useAuth();
  const { count } = useCart();
  const nav = useNavigate();
  return (
    <header className="nav">
      <span className="brand">Cartly</span>
      <nav>
        <NavLink to="/" end>Shop</NavLink>
        {user?.role !== "ADMIN" && <NavLink to="/cart">Cart ({count})</NavLink>}
        {user?.role === "CUSTOMER" && <NavLink to="/orders">My orders</NavLink>}
        {user?.role === "ADMIN" && <NavLink to="/admin">Admin</NavLink>}
        {!user && <NavLink to="/login">Log in</NavLink>}
        {!user && <NavLink to="/register">Sign up</NavLink>}
        {user && (
          <button className="link" onClick={() => { logout(); nav("/"); }}>
            Log out ({user.name.split(" ")[0]})
          </button>
        )}
      </nav>
    </header>
  );
}

export default function App() {
  return (
    <>
      <Navbar />
      <main className="container">
        <Suspense fallback={<p className="muted">Loading...</p>}>
          <Routes>
            <Route path="/" element={<Shop />} />
            <Route path="/login" element={<Auth mode="login" />} />
            <Route path="/register" element={<Auth mode="register" />} />
            <Route path="/cart" element={<Cart />} />
            <Route path="/orders" element={<Protected role="CUSTOMER"><Orders /></Protected>} />
            <Route path="/admin" element={<Protected role="ADMIN"><Admin /></Protected>} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </Suspense>
      </main>
    </>
  );
}