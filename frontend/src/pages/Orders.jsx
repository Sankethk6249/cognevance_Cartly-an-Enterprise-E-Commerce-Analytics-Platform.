import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import api, { errMsg } from "../api";
import { payOrder } from "../pay.js";
import { fmt, fmtDate } from "../util.js";

export default function Orders() {
  const [orders, setOrders] = useState([]);
  const [msg, setMsg] = useState("");

  const load = () => api.get("/orders/my").then((r) => setOrders(r.data)).catch((e) => setMsg(errMsg(e)));
  useEffect(() => { load(); }, []);

  const pay = async (o) => { setMsg(""); try { await payOrder(o); load(); } catch (e) { setMsg(errMsg(e)); } };
  const cancel = async (o) => {
    if (!window.confirm(`Cancel order #${o.id}?`)) return;
    try { await api.put(`/orders/${o.id}/cancel`); load(); } catch (e) { setMsg(errMsg(e)); }
  };

  return (
    <>
      <h1>My orders</h1>
      {msg && <p className="error">{msg}</p>}
      {orders.length === 0 && <p className="muted">No orders yet. <Link to="/">Start shopping</Link></p>}
      {orders.map((o) => (
        <article className="card order" key={o.id}>
          <div className="price-row">
            <strong>Order #{o.id}</strong>
            <span className={`status ${o.status.toLowerCase()}`}>{o.status}</span>
          </div>
          <p className="muted small">{fmtDate(o.createdAt)} - {o.shippingAddress}</p>
          {o.items.map((i) => (
            <p key={i.productId} className="small">{i.quantity} x {i.name} - {fmt(i.price * i.quantity)}</p>
          ))}
          <div className="price-row">
            <strong>{fmt(o.total)}</strong>
            {o.status === "PENDING" && (
              <div className="row">
                <button className="btn" onClick={() => pay(o)}>Pay now</button>
                <button className="btn danger" onClick={() => cancel(o)}>Cancel</button>
              </div>
            )}
          </div>
        </article>
      ))}
    </>
  );
}