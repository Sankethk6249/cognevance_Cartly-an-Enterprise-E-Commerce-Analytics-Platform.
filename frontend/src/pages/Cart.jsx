import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import api, { errMsg } from "../api";
import { useAuth } from "../auth.jsx";
import { useCart } from "../cart.jsx";
import { payOrder } from "../pay.js";
import { fmt } from "../util.js";

export default function Cart() {
  const { user } = useAuth();
  const { items, setQty, remove, clear, total } = useCart();
  const [address, setAddress] = useState("");
  const [err, setErr] = useState("");
  const [busy, setBusy] = useState(false);
  const nav = useNavigate();

  const checkout = async () => {
    setErr("");
    if (!user) return nav("/login");
    if (address.trim().length < 10) return setErr("Enter your full shipping address");
    setBusy(true);
    let order;
    try {
      ({ data: order } = await api.post("/orders", {
        items: items.map((i) => ({ productId: i.id, quantity: i.qty })),
        shippingAddress: address,
      }));
      clear();
      await payOrder(order);
      nav("/orders");
    } catch (e) {
      setErr(errMsg(e));
      if (order) setTimeout(() => nav("/orders"), 2500);
    }
    setBusy(false);
  };

  if (items.length === 0) return (<><h1>Your cart</h1><p className="muted">Your cart is empty. <Link to="/">Browse products</Link></p></>);

  return (
    <>
      <h1>Your cart</h1>
      {items.map((i) => (
        <div className="card cart-row" key={i.id}>
          <img src={i.imageUrl} alt="" loading="lazy" />
          <div className="grow"><strong>{i.name}</strong><p className="muted small">{fmt(i.price)} each</p></div>
          <input type="number" min="1" max={i.stock} value={i.qty} onChange={(e) => setQty(i.id, Number(e.target.value))} aria-label={`Quantity for ${i.name}`} />
          <strong>{fmt(i.price * i.qty)}</strong>
          <button className="btn danger" onClick={() => remove(i.id)}>Remove</button>
        </div>
      ))}
      <div className="card">
        <label>Shipping address
          <textarea rows="3" value={address} onChange={(e) => setAddress(e.target.value)} placeholder="House no, street, city, PIN code" />
        </label>
        {err && <p className="error">{err}</p>}
        <div className="price-row">
          <h3>Total: {fmt(total)}</h3>
          <button className="btn" onClick={checkout} disabled={busy}>
            {busy ? "Processing..." : user ? "Place order and pay" : "Log in to checkout"}
          </button>
        </div>
      </div>
    </>
  );
}