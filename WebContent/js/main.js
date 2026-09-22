// Client-side sort of the already-rendered product grid.
// (Search and category filtering are handled server-side by HomeServlet
// so results always reflect the live MySQL data; this just reorders
// the cards already on the page for a snappier "sort" experience.)

document.addEventListener("DOMContentLoaded", function () {
    const sortSelect = document.getElementById("sortSelect");
    const grid = document.getElementById("productGrid");
    if (!sortSelect || !grid) return;

    sortSelect.addEventListener("change", function () {
        const cards = Array.from(grid.querySelectorAll(".product-card"));
        const mode = sortSelect.value;

        cards.sort(function (a, b) {
            if (mode === "name") {
                return a.dataset.name.localeCompare(b.dataset.name);
            }
            if (mode === "price-asc") {
                const priceA = lowestPrice(a);
                const priceB = lowestPrice(b);
                return priceA - priceB;
            }
            if (mode === "store") {
                const storeA = a.querySelector(".price-row.lowest .store-name")?.textContent || "";
                const storeB = b.querySelector(".price-row.lowest .store-name")?.textContent || "";
                return storeA.localeCompare(storeB);
            }
            return 0;
        });

        cards.forEach(function (card) { grid.appendChild(card); });
    });

    function lowestPrice(card) {
        const rows = Array.from(card.querySelectorAll(".price-row"));
        const prices = rows.map(function (r) { return parseFloat(r.dataset.price); });
        return Math.min.apply(null, prices);
    }
});
