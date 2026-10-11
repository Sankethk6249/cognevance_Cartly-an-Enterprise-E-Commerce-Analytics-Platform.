import axios from "axios";

const api = axios.create({
  baseURL: (import.meta.env.VITE_API_URL || "http://localhost:8080") + "/api",
});

api.interceptors.request.use((cfg) => {
  const token = localStorage.getItem("token");
  if (token) cfg.headers.Authorization = `Bearer ${token}`;
  return cfg;
});

api.interceptors.response.use(
  (r) => r,
  (e) => {
    if (e.response?.status === 401 && localStorage.getItem("token")) {
      localStorage.removeItem("token");
      localStorage.removeItem("user");
      window.location.href = "/login";
    }
    return Promise.reject(e);
  }
);

export const errMsg = (e) => {
  const d = e.response?.data;
  if (d?.errors) return Object.values(d.errors).join(", ");
  return d?.message || e.message || "Cannot reach the server. Please try again.";
};

export default api;