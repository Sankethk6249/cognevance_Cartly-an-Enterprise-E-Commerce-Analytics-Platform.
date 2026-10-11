import { createContext, useContext, useEffect, useState } from "react";

const Ctx = createContext(null);
export const useCart = () => useContext(Ctx);

export function CartProvider({ children }) {
  const [items, setItems] = useState(() => JSON.parse(localStorage.getItem("cart") || "[]"));
  useEffect(() => localStorage.setItem("cart", JSON.stringify(items)), [items]);

  const add = (p) =>
    setItems((cur) => {
      const found = cur.find((i) => i.id === p.id);
      if (found) return cur.map((i) => (i.id === p.id ? { ...i, stock: p.stock, qty: Math.min(i.qty + 1, p.stock) } : i));
      return [...cur, { id: p.id, name: p.name, price: p.price, imageUrl: p.imageUrl, stock: p.stock, qty: 1 }];
    });
  const setQty = (id, q) =>
    setItems((cur) => cur.map((i) => (i.id === id ? { ...i, qty: Math.max(1, Math.min(q, i.stock)) } : i)));
  const remove = (id) => setItems((cur) => cur.filter((i) => i.id !== id));
  const clear = () => setItems([]);
  const total = items.reduce((s, i) => s + i.price * i.qty, 0);
  const count = items.reduce((s, i) => s + i.qty, 0);

  return <Ctx.Provider value={{ items, add, setQty, remove, clear, total, count }}>{children}</Ctx.Provider>;
}