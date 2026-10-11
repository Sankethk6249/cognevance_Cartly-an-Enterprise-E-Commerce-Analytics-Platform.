import { useEffect, useState } from "react";
import api, { errMsg } from "../api";
import { fmt, fmtDate } from "../util.js";

const STATUSES = ["", "PENDING", "PAID", "SHIPPED", "DELIVERED", "CANCELLED"];
const emptyProduct = { name: "", description: "", price: "", stock: 10, category: "", imageUrl: "" };

function Bars({ data, label, value }) {
  const max = Math.max(...data.map((d) => Number(d[value])), 1);
  return (
    <div className="bars">
      {data.map((d) => (
        <div className="bar-col" key={d[label]} title={`${d[label]}: ${fmt(d[value])}`}>
          <div style={{ height: `${(Number(d[value]) / max) * 100}%` }} />
          <span>{String(d[label]).slice(-5)}</span>
        </div>
      ))}
    </div>
  );
}

function Dashboard() {
  const [a, setA] = useState(null);
  const [err, setErr] = useState("");
  useEffect(() => { api.get("/admin/analytics").then((r) => setA(r.data)).catch((e) => setErr(errMsg(e))); }, []);

  const download = async () => {
    const { data } = await api.get("/admin/reports/orders.csv", { responseType: "blob" });
    const url = URL.createObjectURL(data);
    const link = document.createElement("a");
    link.href = url; link.download = "orders.csv"; link.click();
    URL.revokeObjectURL(url);
  };

  if (err) return <p className="error">{err}</p>;
  if (!a) return <p className="muted">Loading analytics...</p>;
  return (
    <>
      <div className="stats">
        <div className="card"><strong>{fmt(a.revenue)}</strong><span>Revenue</span></div>
        <div className="card"><strong>{a.totalOrders}</strong><span>Orders</span></div>
        <div className="card"><strong>{a.customers}</strong><span>Customers</span></div>
        <div className="card"><strong>{a.products}</strong><span>Products</span></div>
      </div>
      <div className="card"><h3>Revenue, last 14 days</h3><Bars data={a.salesByDay} label="date" value="revenue" /></div>
      <div className="two">
        <div className="card"><h3>Orders by status</h3>
          {Object.entries(a.ordersByStatus).map(([s, n]) => (
            <div className="price-row" key={s}><span className={`status ${s.toLowerCase()}`}>{s}</span><strong>{n}</strong></div>
          ))}
        </div>
        <div className="card"><h3>Revenue by category</h3>
          {a.salesByCategory.length === 0 ? <p className="muted">No sales yet.</p> : <Bars data={a.salesByCategory} label="category" value="revenue" />}
        </div>
      </div>
      <div className="two">
        <div className="card"><h3>Top products</h3>
          {a.topProducts.length === 0 && <p className="muted">No sales yet.</p>}
          {a.topProducts.map((p) => (<div className="price-row" key={p.name}><span>{p.name}</span><span>{p.units} sold - {fmt(p.revenue)}</span></div>))}
        </div>
        <div className="card"><h3>Low stock</h3>
          {a.lowStock.length === 0 && <p className="muted">All products are well stocked.</p>}
          {a.lowStock.map((p) => (<div className="price-row" key={p.id}><span>{p.name}</span><strong className="warn">{p.stock} left</strong></div>))}
        </div>
      </div>
      <button className="btn" onClick={download}>Download orders report (CSV)</button>
    </>
  );
}

function Products() {
  const [list, setList] = useState([]);
  const [form, setForm] = useState(emptyProduct);
  const [editId, setEditId] = useState(null);
  const [msg, setMsg] = useState({ type: "", text: "" });
  const [uploading, setUploading] = useState(false);

  const load = () => api.get("/products", { params: { size: 50 } }).then((r) => setList(r.data.content)).catch((e) => setMsg({ type: "error", text: errMsg(e) }));
  useEffect(() => { load(); }, []);
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const upload = async (e) => {
    const file = e.target.files[0];
    if (!file) return;
    setUploading(true);
    const fd = new FormData();
    fd.append("file", file);
    try {
      const { data } = await api.post("/products/upload", fd);
      setForm((f) => ({ ...f, imageUrl: data.url }));
    } catch (x) { setMsg({ type: "error", text: errMsg(x) }); }
    setUploading(false);
  };

  const save = async (e) => {
    e.preventDefault();
    if (Number(form.price) <= 0) return setMsg({ type: "error", text: "Price must be greater than 0" });
    const body = { ...form, price: Number(form.price), stock: Number(form.stock) };
    try {
      editId ? await api.put(`/products/${editId}`, body) : await api.post("/products", body);
      setMsg({ type: "notice", text: editId ? "Product updated" : "Product created" });
      setForm(emptyProduct); setEditId(null); load();
    } catch (x) { setMsg({ type: "error", text: errMsg(x) }); }
  };

  const edit = (p) => {
    setEditId(p.id);
    setForm({ name: p.name, description: p.description, price: p.price, stock: p.stock, category: p.category, imageUrl: p.imageUrl || "" });
    window.scrollTo({ top: 0, behavior: "smooth" });
  };
  const del = async (p) => {
    if (!window.confirm(`Remove "${p.name}" from the shop?`)) return;
    try { await api.delete(`/products/${p.id}`); load(); } catch (x) { setMsg({ type: "error", text: errMsg(x) }); }
  };

  return (
    <>
      {msg.text && <p className={msg.type}>{msg.text}</p>}
      <form className="card form" onSubmit={save}>
        <h3>{editId ? "Edit product" : "Add a product"}</h3>
        <label>Name<input required value={form.name} onChange={set("name")} /></label>
        <label>Description<textarea required rows="2" value={form.description} onChange={set("description")} /></label>
        <div className="row">
          <label>Price (₹)<input type="number" step="0.01" min="0.01" required value={form.price} onChange={set("price")} /></label>
          <label>Stock<input type="number" min="0" required value={form.stock} onChange={set("stock")} /></label>
          <label>Category<input required value={form.category} onChange={set("category")} /></label>
        </div>
        <label>Image<input type="file" accept="image/*" onChange={upload} /></label>
        {uploading && <p className="muted small">Uploading...</p>}
        {form.imageUrl && <img className="preview" src={form.imageUrl} alt="Preview" />}
        <div className="row">
          <button className="btn" type="submit" disabled={uploading}>{editId ? "Save changes" : "Create product"}</button>
          {editId && <button type="button" className="btn ghost" onClick={() => { setEditId(null); setForm(emptyProduct); }}>Cancel</button>}
        </div>
      </form>
      <div className="scroll"><table>
        <thead><tr><th>Name</th><th>Category</th><th>Price</th><th>Stock</th><th></th></tr></thead>
        <tbody>{list.map((p) => (
          <tr key={p.id}><td>{p.name}</td><td>{p.category}</td><td>{fmt(p.price)}</td><td>{p.stock}</td>
            <td className="actions"><button className="btn ghost" onClick={() => edit(p)}>Edit</button><button className="btn danger" onClick={() => del(p)}>Remove</button></td></tr>
        ))}</tbody>
      </table></div>
    </>
  );
}

function Orders() {
  const [data, setData] = useState({ content: [], page: 0, totalPages: 0 });
  const [status, setStatus] = useState("");
  const [page, setPage] = useState(0);
  const [msg, setMsg] = useState("");

  const load = () => api.get("/orders", { params: { status: status || undefined, page, size: 10 } })
    .then((r) => setData(r.data)).catch((e) => setMsg(errMsg(e)));
  useEffect(() => { load(); }, [status, page]);

  const move = async (o, next) => {
    setMsg("");
    try { await api.put(`/orders/${o.id}/status`, { status: next }); load(); } catch (e) { setMsg(errMsg(e)); }
  };

  return (
    <>
      <div className="filters one">
        <select value={status} onChange={(e) => { setStatus(e.target.value); setPage(0); }}>
          {STATUSES.map((s) => <option key={s} value={s}>{s || "All statuses"}</option>)}
        </select>
      </div>
      {msg && <p className="error">{msg}</p>}
      <div className="scroll"><table>
        <thead><tr><th>#</th><th>Date</th><th>Customer</th><th>Total</th><th>Status</th><th></th></tr></thead>
        <tbody>{data.content.map((o) => (
          <tr key={o.id}><td>{o.id}</td><td>{fmtDate(o.createdAt)}</td><td>{o.customerName}</td><td>{fmt(o.total)}</td>
            <td><span className={`status ${o.status.toLowerCase()}`}>{o.status}</span></td>
            <td className="actions">
              {o.status === "PAID" && <button className="btn" onClick={() => move(o, "SHIPPED")}>Ship</button>}
              {o.status === "SHIPPED" && <button className="btn" onClick={() => move(o, "DELIVERED")}>Delivered</button>}
              {["PENDING", "PAID", "SHIPPED"].includes(o.status) && <button className="btn danger" onClick={() => move(o, "CANCELLED")}>Cancel</button>}
            </td></tr>
        ))}</tbody>
      </table>{data.content.length === 0 && <p className="muted">No orders found.</p>}</div>
      {data.totalPages > 1 && (
        <div className="pager">
          <button className="btn ghost" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</button>
          <span>Page {data.page + 1} of {data.totalPages}</span>
          <button className="btn ghost" disabled={page + 1 >= data.totalPages} onClick={() => setPage(page + 1)}>Next</button>
        </div>
      )}
    </>
  );
}

function Customers() {
  const [list, setList] = useState([]);
  useEffect(() => { api.get("/admin/customers").then((r) => setList(r.data)); }, []);
  return (
    <div className="scroll"><table>
      <thead><tr><th>Name</th><th>Email</th><th>Joined</th><th>Orders</th><th>Total spent</th></tr></thead>
      <tbody>{list.map((c) => (
        <tr key={c.id}><td>{c.name}</td><td>{c.email}</td><td>{fmtDate(c.joined)}</td><td>{c.orders}</td><td>{fmt(c.spent)}</td></tr>
      ))}</tbody>
    </table>{list.length === 0 && <p className="muted">No customers yet.</p>}</div>
  );
}

const TABS = { Dashboard, Products, Orders, Customers };

export default function Admin() {
  const [tab, setTab] = useState("Dashboard");
  const View = TABS[tab];
  return (
    <>
      <h1>Admin</h1>
      <div className="tabs">
        {Object.keys(TABS).map((t) => (
          <button key={t} className={tab === t ? "active" : ""} onClick={() => setTab(t)}>{t}</button>
        ))}
      </div>
      <View />
    </>
  );
}