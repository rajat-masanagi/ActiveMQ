# Lab 8 — React frontend

## Goal

Handwrite a five-file React application. It uses only `useState`, `useEffect`,
and `fetch` to create and list orders.

## 1. Create the source directory

From the repository root:

```powershell
New-Item -ItemType Directory -Force frontend/src
Remove-Item frontend/.gitkeep
```

## 2. Write the package file

Create `frontend/package.json`:

```json
{
  "name": "order-frontend",
  "private": true,
  "version": "0.0.1",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vite build --minify false"
  },
  "dependencies": {
    "react": "19.2.0",
    "react-dom": "19.2.0"
  },
  "devDependencies": {
    "vite": "8.1.0"
  }
}
```

`react` provides components and hooks. `react-dom` mounts the application in the
browser. Vite is the development server and production builder. The learning
build disables minification so it does not depend on a platform-native minifier;
the JavaScript and CSS are still bundled. No router, testing library, CSS package,
or React plugin is required for this small JSX app.

## 3. Write the HTML entry point

Create `frontend/index.html`:

```html
<!doctype html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Order Lab</title>
  </head>
  <body>
    <div id="root"></div>
    <script type="module" src="/src/main.jsx"></script>
  </body>
</html>
```

Vite treats `index.html` as an entry point. React will render inside the empty
`root` element.

## 4. Mount React

Create `frontend/src/main.jsx`:

```jsx
import { createRoot } from "react-dom/client";
import App from "./App.jsx";
import "./styles.css";

createRoot(document.getElementById("root")).render(<App />);
```

`createRoot` connects React to the existing DOM element. Imports assemble the
component and its plain CSS.

## 5. Write the application component

Create `frontend/src/App.jsx`:

```jsx
import { useEffect, useState } from "react";

const API_URL = "http://localhost:8081/orders";
const EMPTY_FORM = { customerEmail: "", product: "", quantity: 1 };

export default function App() {
  const [form, setForm] = useState(EMPTY_FORM);
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  async function loadOrders() {
    setLoading(true);
    setError("");

    try {
      const response = await fetch(API_URL);
      if (!response.ok) throw new Error(`Could not load orders (${response.status})`);
      setOrders(await response.json());
    } catch (problem) {
      setError(problem.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadOrders();
  }, []);

  function changeField(event) {
    const { name, value } = event.target;
    setForm({
      ...form,
      [name]: name === "quantity" ? Number(value) : value
    });
  }

  async function submitOrder(event) {
    event.preventDefault();
    setSubmitting(true);
    setError("");

    try {
      const response = await fetch(API_URL, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(form)
      });

      if (!response.ok) throw new Error(`Could not create order (${response.status})`);
      setForm(EMPTY_FORM);
      await loadOrders();
    } catch (problem) {
      setError(problem.message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main>
      <h1>Order lab</h1>
      <p>Create an order and watch Email Service consume its event.</p>

      <form onSubmit={submitOrder}>
        <label>
          Customer email
          <input
            name="customerEmail"
            type="email"
            value={form.customerEmail}
            onChange={changeField}
            required
          />
        </label>

        <label>
          Product
          <input
            name="product"
            value={form.product}
            onChange={changeField}
            required
          />
        </label>

        <label>
          Quantity
          <input
            name="quantity"
            type="number"
            min="1"
            value={form.quantity}
            onChange={changeField}
            required
          />
        </label>

        <button disabled={submitting}>
          {submitting ? "Creating..." : "Create order"}
        </button>
      </form>

      {error && <p className="error">{error}</p>}

      <section>
        <div className="heading-row">
          <h2>Orders</h2>
          <button className="secondary" onClick={loadOrders}>Refresh</button>
        </div>

        {loading ? (
          <p>Loading...</p>
        ) : orders.length === 0 ? (
          <p>No orders yet.</p>
        ) : (
          <ul>
            {orders.map((order) => (
              <li key={order.id}>
                <strong>{order.product}</strong> × {order.quantity}
                <span>{order.customerEmail}</span>
              </li>
            ))}
          </ul>
        )}
      </section>
    </main>
  );
}
```

`useState` stores form, server data, and UI status. `useEffect` performs the first
GET after mounting. `fetch` makes the only two API calls: GET and POST. A
controlled input gets its value from state and calls `changeField` on edits.
Spreading `form` preserves the other fields. Conditional JSX selects loading,
empty, error, and populated states.

## 6. Add small, plain styling

Create `frontend/src/styles.css`:

```css
:root {
  font-family: system-ui, sans-serif;
  color: #172033;
  background: #f4f6fb;
}

body {
  margin: 0;
}

main {
  width: min(680px, calc(100% - 32px));
  margin: 48px auto;
}

form, section {
  display: grid;
  gap: 16px;
  margin-top: 24px;
  padding: 24px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 8px 24px #1b2b4b14;
}

label {
  display: grid;
  gap: 6px;
  font-weight: 600;
}

input, button {
  padding: 10px 12px;
  font: inherit;
}

button {
  border: 0;
  border-radius: 8px;
  color: white;
  background: #3157d5;
  cursor: pointer;
}

button:disabled {
  opacity: 0.6;
}

.secondary {
  color: #3157d5;
  background: #e9edff;
}

.heading-row, li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

ul {
  display: grid;
  gap: 12px;
  margin: 0;
  padding: 0;
  list-style: none;
}

li {
  padding-top: 12px;
  border-top: 1px solid #e4e8f0;
}

li span {
  color: #647087;
}

.error {
  color: #b42318;
}
```

## 7. Install and run

Keep Order Service running, then:

```powershell
Set-Location frontend
npm.cmd install
npm.cmd run dev
```

Open <http://localhost:5173>. Submit an order and confirm it appears in the list.
Watch the Email Service terminal for its simulated email.

`npm.cmd install` generates `package-lock.json` and `node_modules`; you manually wrote
the application files, while npm manages installed packages.

## Checkpoint

The page loads existing orders, creates a valid order, refreshes the list, and
shows an error if Order Service is stopped. Browser developer tools show calls
only to GET and POST `/orders`.

## Troubleshooting

- A browser CORS error usually means the backend origin differs from exactly
  `http://localhost:5173` or the controller lacks `@CrossOrigin`.
- `Failed to fetch` means Order Service is unavailable or the API URL is wrong.
- If Vite reports an unsupported Node version, upgrade Node to a version accepted
  by Vite 8.1.
- If JSX is shown as text or cannot be parsed, confirm the files use the `.jsx`
  extension and `package.json` contains `"type": "module"`.
