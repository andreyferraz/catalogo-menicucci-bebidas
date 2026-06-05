const WHATSAPP_NUMBER = document.body.dataset.whatsappNumber || window.CATALOGO_WHATSAPP || "5528988079115";
const CART_STORAGE_KEY = "bebidas-menicucci-cart";
const CUSTOMER_STORAGE_KEY = "bebidas-menicucci-customer";
const PRODUCTS_PER_PAGE = 16;

const products = (window.CATALOGO_PRODUCTS || []).map((product) => ({
  id: product.id,
  name: product.nome,
  category: product.categoria || "Sem categoria",
  description: product.descricao || "",
  price: Number(product.preco || 0),
  imageUrl: normalizeImageUrl(product.imagemUrl || product.imagem_url),
  icon: pickProductIcon(product.categoria, product.nome)
}));

let selectedCategory = "Todos";
let searchTerm = "";
let currentProductsPage = 1;
let cart = loadCart();

const formatCurrency = (value) => value.toLocaleString("pt-BR", {
  style: "currency",
  currency: "BRL"
});

function pickProductIcon(category, name) {
  const text = `${category || ""} ${name || ""}`.toLowerCase();

  if (text.includes("vinho")) return "🍷";
  if (text.includes("cerveja")) return "🍺";
  if (text.includes("whisky") || text.includes("uísque") || text.includes("destil")) return "🥃";
  if (text.includes("gin") || text.includes("vodka") || text.includes("vodca")) return "🍸";
  if (text.includes("água") || text.includes("agua") || text.includes("sem álcool") || text.includes("sem alcool")) return "💧";
  if (text.includes("refrigerante") || text.includes("suco")) return "🥤";

  return "🍾";
}

function normalizeImageUrl(imageUrl) {
  if (!imageUrl) return "";

  if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
    return imageUrl;
  }

  if (imageUrl.startsWith("/")) {
    return imageUrl;
  }

  if (imageUrl.startsWith("uploads/")) {
    return `/${imageUrl}`;
  }

  return `/uploads/${imageUrl}`;
}

function loadCart() {
  try {
    return JSON.parse(localStorage.getItem(CART_STORAGE_KEY)) || [];
  } catch {
    return [];
  }
}

function saveCart() {
  localStorage.setItem(CART_STORAGE_KEY, JSON.stringify(cart));
  updateCartUI();
}

function loadCustomer() {
  try {
    const data = JSON.parse(localStorage.getItem(CUSTOMER_STORAGE_KEY)) || {};
    document.getElementById("customerName").value = data.name || "";
    document.getElementById("customerAddress").value = data.address || "";
  } catch {
    // mantém campos vazios
  }
}

function saveCustomer() {
  localStorage.setItem(CUSTOMER_STORAGE_KEY, JSON.stringify({
    name: document.getElementById("customerName").value.trim(),
    address: document.getElementById("customerAddress").value.trim()
  }));
}

function getProduct(productId) {
  return products.find((product) => product.id === productId);
}

function addToCart(productId) {
  const existingItem = cart.find((item) => item.productId === productId);

  if (existingItem) {
    existingItem.quantity += 1;
  } else {
    cart.push({ productId, quantity: 1 });
  }

  saveCart();
  showToast("Produto adicionado ao carrinho");
}

function changeQuantity(productId, amount) {
  const item = cart.find((cartItem) => cartItem.productId === productId);
  if (!item) return;

  item.quantity += amount;

  if (item.quantity <= 0) {
    cart = cart.filter((cartItem) => cartItem.productId !== productId);
  }

  saveCart();
}

function removeFromCart(productId) {
  cart = cart.filter((item) => item.productId !== productId);
  saveCart();
}

function clearCart() {
  if (!cart.length) return;

  const confirmed = confirm("Deseja realmente limpar o carrinho?");
  if (!confirmed) return;

  cart = [];
  saveCart();
  showToast("Carrinho limpo");
}

function getCartTotal() {
  return cart.reduce((total, item) => {
    const product = getProduct(item.productId);
    return total + (product ? product.price * item.quantity : 0);
  }, 0);
}

function getCartItemsCount() {
  return cart.reduce((total, item) => total + item.quantity, 0);
}

function renderCategories() {
  const categories = ["Todos", ...new Set(products.map((product) => product.category))];
  const tabs = document.getElementById("categoryTabs");

  tabs.innerHTML = categories.map((category) => `
    <button type="button" class="${category === selectedCategory ? "active" : ""}" data-category="${category}">
      ${category}
    </button>
  `).join("");

  tabs.querySelectorAll("button").forEach((button) => {
    button.addEventListener("click", () => {
      selectedCategory = button.dataset.category;
      currentProductsPage = 1;
      renderCategories();
      renderProducts();
    });
  });
}

function getFilteredProducts() {
  const normalizedSearch = searchTerm.trim().toLowerCase();

  return products.filter((product) => {
    const matchesCategory = selectedCategory === "Todos" || product.category === selectedCategory;
    const matchesSearch = !normalizedSearch
      || product.name.toLowerCase().includes(normalizedSearch)
      || product.description.toLowerCase().includes(normalizedSearch)
      || product.category.toLowerCase().includes(normalizedSearch);

    return matchesCategory && matchesSearch;
  });
}

function renderProductsPagination(totalProducts) {
  const pagination = document.getElementById("productsPagination");
  const totalPages = Math.max(1, Math.ceil(totalProducts / PRODUCTS_PER_PAGE));

  if (totalProducts <= PRODUCTS_PER_PAGE) {
    pagination.innerHTML = "";
    pagination.hidden = true;
    return;
  }

  currentProductsPage = Math.min(currentProductsPage, totalPages);
  pagination.hidden = false;

  pagination.innerHTML = `
    <button type="button" class="pagination-button" data-page-nav="prev" ${currentProductsPage === 1 ? "disabled" : ""}>
      Anterior
    </button>
    <div class="pagination-pages">
      ${Array.from({ length: totalPages }, (_, index) => {
        const page = index + 1;
        return `<button type="button" class="pagination-page ${page === currentProductsPage ? "active" : ""}" data-page-number="${page}">${page}</button>`;
      }).join("")}
    </div>
    <button type="button" class="pagination-button" data-page-nav="next" ${currentProductsPage === totalPages ? "disabled" : ""}>
      Próxima
    </button>
  `;

  pagination.querySelectorAll("[data-page-number]").forEach((button) => {
    button.addEventListener("click", () => {
      currentProductsPage = Number(button.dataset.pageNumber);
      renderProducts();
    });
  });

  pagination.querySelectorAll("[data-page-nav]").forEach((button) => {
    button.addEventListener("click", () => {
      const direction = button.dataset.pageNav;
      if (direction === "prev" && currentProductsPage > 1) {
        currentProductsPage -= 1;
      }
      if (direction === "next" && currentProductsPage < totalPages) {
        currentProductsPage += 1;
      }
      renderProducts();
    });
  });
}

function renderProducts() {
  const productsGrid = document.getElementById("productsGrid");
  const filteredProducts = getFilteredProducts();
  const totalPages = Math.max(1, Math.ceil(filteredProducts.length / PRODUCTS_PER_PAGE));
  const safePage = Math.min(currentProductsPage, totalPages);
  const startIndex = (safePage - 1) * PRODUCTS_PER_PAGE;
  const visibleProducts = filteredProducts.slice(startIndex, startIndex + PRODUCTS_PER_PAGE);

  currentProductsPage = safePage;

  if (!filteredProducts.length) {
    productsGrid.innerHTML = `<div class="empty-cart">Nenhum produto encontrado.</div>`;
    renderProductsPagination(0);
    return;
  }

  productsGrid.innerHTML = visibleProducts.map((product) => `
    <article class="product-card">
      <div class="product-image">
        ${product.imageUrl
          ? `<img class="product-photo" src="${product.imageUrl}" alt="${product.name}" />`
          : `<span class="product-icon">${product.icon}</span>`}
      </div>
      <div class="product-body">
        <span class="product-category">${product.category}</span>
        <h3>${product.name}</h3>
        <p>${product.description}</p>
        <div class="product-footer">
          <strong class="price">${formatCurrency(product.price)}</strong>
          <button class="add-button" type="button" data-add-product="${product.id}">Adicionar</button>
        </div>
      </div>
    </article>
  `).join("");

  productsGrid.querySelectorAll("[data-add-product]").forEach((button) => {
    button.addEventListener("click", () => addToCart(button.dataset.addProduct));
  });

  renderProductsPagination(filteredProducts.length);
}

function updateCartUI() {
  document.getElementById("cartCount").textContent = getCartItemsCount();
  document.getElementById("cartTotal").textContent = formatCurrency(getCartTotal());

  const cartItems = document.getElementById("cartItems");

  if (!cart.length) {
    cartItems.innerHTML = `<div class="empty-cart">Seu carrinho está vazio. Adicione produtos para montar o pedido.</div>`;
    return;
  }

  cartItems.innerHTML = cart.map((item) => {
    const product = getProduct(item.productId);
    if (!product) return "";

    return `
      <div class="cart-item">
        <div class="cart-item-icon">${product.icon}</div>
        <div>
          <h3>${product.name}</h3>
          <small>${formatCurrency(product.price)} cada</small>
          <div class="quantity-control">
            <button type="button" data-decrease="${product.id}" aria-label="Diminuir quantidade">−</button>
            <strong>${item.quantity}</strong>
            <button type="button" data-increase="${product.id}" aria-label="Aumentar quantidade">+</button>
          </div>
        </div>
        <button class="remove-button" type="button" data-remove="${product.id}">Remover</button>
      </div>
    `;
  }).join("");

  cartItems.querySelectorAll("[data-increase]").forEach((button) => {
    button.addEventListener("click", () => changeQuantity(button.dataset.increase, 1));
  });

  cartItems.querySelectorAll("[data-decrease]").forEach((button) => {
    button.addEventListener("click", () => changeQuantity(button.dataset.decrease, -1));
  });

  cartItems.querySelectorAll("[data-remove]").forEach((button) => {
    button.addEventListener("click", () => removeFromCart(button.dataset.remove));
  });
}

function openCart() {
  document.getElementById("cartDrawer").classList.add("open");
  document.getElementById("cartDrawer").setAttribute("aria-hidden", "false");
}

function closeCart() {
  document.getElementById("cartDrawer").classList.remove("open");
  document.getElementById("cartDrawer").setAttribute("aria-hidden", "true");
}

function sendOrderToWhatsApp() {
  if (!cart.length) {
    showToast("Adicione produtos ao carrinho antes de solicitar o pedido");
    return;
  }

  saveCustomer();

  const customerName = document.getElementById("customerName").value.trim();
  const customerAddress = document.getElementById("customerAddress").value.trim();

  const lines = [
    "Olá, Bebidas Menicucci! Gostaria de solicitar um pedido:",
    "",
    ...cart.map((item) => {
      const product = getProduct(item.productId);
      if (!product) return "";
      const subtotal = product.price * item.quantity;
      return `• ${item.quantity}x ${product.name} - ${formatCurrency(product.price)} | Subtotal: ${formatCurrency(subtotal)}`;
    }).filter(Boolean),
    "",
    `Total estimado: ${formatCurrency(getCartTotal())}`,
    customerName ? `Nome: ${customerName}` : "Nome: não informado",
    customerAddress ? `Observação/Endereço: ${customerAddress}` : "Observação/Endereço: não informado"
  ];

  const message = encodeURIComponent(lines.join("\n"));
  window.open(`https://wa.me/${WHATSAPP_NUMBER}?text=${message}`, "_blank");
}

function showToast(message) {
  const toast = document.getElementById("toast");
  toast.textContent = message;
  toast.classList.add("show");
  window.clearTimeout(showToast.timeoutId);
  showToast.timeoutId = window.setTimeout(() => toast.classList.remove("show"), 2200);
}

function init() {
  const currentYearElement = document.getElementById("copyrightCurrentYear");
  if (currentYearElement) {
    currentYearElement.textContent = String(new Date().getFullYear());
  }

  renderCategories();
  renderProducts();
  updateCartUI();
  loadCustomer();

  document.getElementById("searchInput").addEventListener("input", (event) => {
    searchTerm = event.target.value;
    currentProductsPage = 1;
    renderProducts();
  });

  document.getElementById("openCartBtn").addEventListener("click", openCart);
  document.getElementById("heroCartBtn").addEventListener("click", openCart);
  document.getElementById("closeCartBtn").addEventListener("click", closeCart);
  document.getElementById("closeCartOverlay").addEventListener("click", closeCart);
  document.getElementById("clearCartBtn").addEventListener("click", clearCart);
  document.getElementById("sendOrderBtn").addEventListener("click", sendOrderToWhatsApp);
  document.getElementById("customerName").addEventListener("input", saveCustomer);
  document.getElementById("customerAddress").addEventListener("input", saveCustomer);

  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape") closeCart();
  });
}

init();
