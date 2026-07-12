/**
 * Shared price-editing mixin untuk halaman denom (per-produk & semua-denom).
 * Host component harus punya: getDenoms() (via arg), formatRp(v), loadDenoms(),
 * dan Alpine store 'toast'. Spread hasil factory ke return object komponen.
 */
window.denomPriceEditing = function (getDenoms) {
    return {
        dirty: {},
        showPriceModal: false,
        savingPrices: false,
        priceResults: [],
        filterUnpriced: false,

        get dirtyList() { return Object.values(this.dirty); },
        get unpricedCount() {
            return this.denoms.filter(d => !d.deleted && d.price == null).length;
        },
        isDirty(d) { return !!this.dirty[d.id]; },
        pendingPrice(d) { return this.dirty[d.id] ? this.dirty[d.id].newPrice : d.price; },
        resultFor(id) { return this.priceResults.find(r => r.id === id); },

        setPrice(d, val) {
            const p = val === '' || val == null ? null : Number(val);
            const unchanged = p === null || (d.price != null && Number(d.price) === p);
            if (unchanged) {
                delete this.dirty[d.id];
            } else {
                this.dirty[d.id] = { id: d.id, code: d.code, name: d.name, oldPrice: d.price, newPrice: p };
            }
        },

        openPriceModal() {
            this.priceResults = [];
            this.showPriceModal = true;
        },

        async submitPrices() {
            this.savingPrices = true;
            try {
                const res = await fetch('/api/admin/catalog/denoms/prices', {
                    method: 'PUT', headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(this.dirtyList.map(x => ({ id: x.id, price: x.newPrice })))
                });
                if (!res.ok) throw new Error('Gagal menyimpan harga');
                this.priceResults = await res.json();
                const okCount = this.priceResults.filter(r => r.ok).length;
                const failCount = this.priceResults.length - okCount;
                for (const r of this.priceResults) {
                    if (r.ok) delete this.dirty[r.id];
                }
                await this.loadDenoms();
                if (failCount === 0) {
                    this.showPriceModal = false;
                    Alpine.store('toast').success(`${okCount} harga diperbarui`);
                } else {
                    Alpine.store('toast').error(`${failCount} gagal disimpan, ${okCount} sukses`);
                }
            } catch (e) {
                Alpine.store('toast').error(e.message);
            } finally {
                this.savingPrices = false;
            }
        }
    };
};
