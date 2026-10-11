import api from "./api";

const loadRazorpay = () =>
  new Promise((resolve, reject) => {
    if (window.Razorpay) return resolve();
    const s = document.createElement("script");
    s.src = "https://checkout.razorpay.com/v1/checkout.js";
    s.onload = resolve;
    s.onerror = () => reject(new Error("Could not load the payment gateway"));
    document.body.appendChild(s);
  });

/** Pays a PENDING order. Uses Razorpay checkout when the server is configured for it, otherwise a demo payment. */
export async function payOrder(order) {
  const { data: pay } = await api.post(`/payments/${order.id}/init`);
  if (pay.provider !== "razorpay") {
    await api.post(`/payments/${order.id}/verify`, { paymentId: "mock_pay_" + Date.now(), signature: "mock" });
    return;
  }
  await loadRazorpay();
  await new Promise((resolve, reject) => {
    const rzp = new window.Razorpay({
      key: pay.keyId,
      amount: pay.amount,
      currency: "INR",
      name: "Cartly",
      order_id: pay.gatewayOrderId,
      handler: async (r) => {
        try {
          await api.post(`/payments/${order.id}/verify`, { paymentId: r.razorpay_payment_id, signature: r.razorpay_signature });
          resolve();
        } catch (e) { reject(e); }
      },
      modal: { ondismiss: () => reject(new Error("Payment not completed. Your order is saved; pay from My orders.")) },
    });
    rzp.open();
  });
}