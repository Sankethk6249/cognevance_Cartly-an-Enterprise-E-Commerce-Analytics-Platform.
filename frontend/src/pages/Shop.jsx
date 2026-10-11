import { useEffect, useState } from "react";
import api, { errMsg } from "../api";
import { useAuth } from "../auth.jsx";
import { useCart } from "../cart.jsx";
import { fmt } from "../util.js";

export default function Shop() {
  const { user } = useAuth();
  const { add } = useCart();
  const [data, setData] = useState({ content: [], page: 0, totalPages: 0, totalElements: 0 });
  const [categories, setCategories] = useState([]);
  const [q, setQ] = useState({ search: "", category: "", sort: "newest", page: 0 });
  const [msg, setMsg] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => { api.get("/products/categories").then((r) => setCategories(r.data)).catch(() => {}); }, []);

  useEffect(() => {
    const t = setTimeout(async () => {
      setLoading(true);
      try {
        const { data } = await api.get("/products", { params: { ...q, size: 12 } });
        setData(data);
        setMsg("");
      } catch (e) { setMsg(errMsg(e)); }
      setLoading(false);
    }, 300);
    return () => clearTimeout(t);
  }, [q]);

  const change = (patch) => setQ({ ...q, page: 0, ...patch });

  return (
    <>
      <h1>Shop</h1>
      <div className="filters">
        <input placeholder="Search products" value={q.search} onChange={(e) => change({ search: e.target.value })} />
        <select value={q.category} onChange={(e) => change({ category: e.target.value })}>
          <option value="">All categories</option>
          {categories.map((c) => <option key={c}>{c}</option>)}
        </select>
        <select value={q.sort} onChange={(e) => change({ sort: e.target.value })}>
          <option value="newest">Newest</option>
          <option value="priceAsc">Price: low to high</option>
          <option value="priceDesc">Price: high to low</option>
        </select>
      </div>
      {msg && <p className="error">{msg}</p>}
      {!loading && data.content.length === 0 && <p className="muted">No products match. Try a different search.</p>}
      <div className="grid">
        {data.content.map((p) => (
          <article className="card product" key={p.id}>
            <img src={p.imageUrl || "https://picsum.photos/seed/placeholder/400/300"} alt={p.name} loading="lazy" />
            <span className="tag">{p.category}</span>
            <h3>{p.name}</h3>
            <p className="muted small">{p.description}</p>
            <div className="price-row">
              <strong>{fmt(p.price)}</strong>
              {p.stock === 0 ? <span className="error small">Out of stock</span>
                : p.stock < 5 ? <span className="warn small">Only {p.stock} left</span> : null}
            </div>
            {user?.role !== "ADMIN" && (
              <button className="btn" disabled={p.stock === 0} onClick={() => { add(p); setMsg(""); }}>Add to cart</button>
            )}
          </article>
        ))}
      </div>
      {data.totalPages > 1 && (
        <div className="pager">
          <button className="btn ghost" disabled={q.page === 0} onClick={() => setQ({ ...q, page: q.page - 1 })}>Previous</button>
          <span>Page {data.page + 1} of {data.totalPages}</span>
          <button className="btn ghost" disabled={q.page + 1 >= data.totalPages} onClick={() => setQ({ ...q, page: q.page + 1 })}>Next</button>
        </div>
      )}
    </>
  );
}