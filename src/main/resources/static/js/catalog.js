"use strict";

const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

function requestHeaders() {
    const headers = { Accept: "application/json" };
    if (csrfHeader && csrfToken) headers[csrfHeader] = csrfToken;
    return headers;
}

document.querySelectorAll("[data-cover]").forEach(image => {
    try {
        const url = new URL(image.dataset.cover);
        if (!["http:", "https:"].includes(url.protocol)) return;
        image.addEventListener("load", () => {
            image.classList.add("catalog-cover-loaded");
            image.previousElementSibling.hidden = true;
        }, { once: true });
        image.src = url.href;
    } catch {
        // La portada alternativa sigue disponible.
    }
});

document.querySelectorAll("[data-list-url]").forEach(button => {
    button.hidden = false;
    button.addEventListener("click", async () => {
        const feedback = document.querySelector("#catalog-feedback");
        button.disabled = true;
        try {
            const response = await fetch(button.dataset.listUrl, {
                method: button.dataset.listMethod, headers: requestHeaders()
            });
            if (!response.ok || response.redirected) throw new Error();
            feedback.textContent = button.dataset.listMethod === "PUT"
                ? "Agregado a Mi lista." : "Quitado de Mi lista.";
        } catch {
            feedback.textContent = "No se pudo actualizar Mi lista. Verifica tu sesión y vuelve a intentar.";
        } finally {
            button.disabled = false;
        }
    });
});

const editor = document.querySelector("#catalog-editor");
if (editor) initEditor(editor);

function initEditor(form) {
    const initial = form.dataset.initial ? JSON.parse(form.dataset.initial) : {};
    const type = form.elements.type;
    const save = form.querySelector("[type=submit]");
    const feedback = document.querySelector("#catalog-form-error");
    const fields = new Map();
    const rowInputs = row => row.querySelectorAll(":scope > label > [data-key], :scope > [data-key]");
    const rowGroup = (parent, key) => parent.querySelector('[data-rows="' + key + '"]');

    function addRow(group, entry = {}) {
        const name = group.dataset.rows;
        const template = document.querySelector("#row-" + name);
        const row = template.content.firstElementChild.cloneNode(true);
        group.appendChild(row);
        rowInputs(row).forEach(input => {
            input.value = entry[input.dataset.key] ?? "";
        });
        if (name === "seasons") {
            (entry.episodes ?? []).forEach(episode => addRow(rowGroup(row, "episodes"), episode));
            if (!entry.episodes?.length) addRow(rowGroup(row, "episodes"));
        }
        if (entry.id || entry.episodes?.some(episode => episode.id)) {
            row.querySelector(':scope > label > [data-key="number"]').readOnly = true;
            row.querySelector(":scope > [data-remove]").disabled = true;
        }
        return row;
    }

    function showType() {
        form.querySelectorAll("[data-types]").forEach(section => {
            const visible = section.dataset.types.split(" ").includes(type.value);
            section.hidden = !visible;
            section.disabled = !visible;
        });
        const seasons = rowGroup(form, "seasons");
        if (type.value === "series" && !seasons.children.length) addRow(seasons);
    }

    function readField(input, key) {
        fields.set(key, input);
        if (input.hasAttribute("data-list")) {
            return input.value.split(",").map(value => value.trim()).filter(Boolean);
        }
        if (input.type === "number") return input.value ? Number(input.value) : null;
        if (input.type === "hidden" && !input.value) return null;
        return input.value;
    }

    function readRows(group, key) {
        return Array.from(group.children, (row, index) => {
            const path = key + "[" + index + "]";
            const entry = {};
            rowInputs(row).forEach(input => {
                entry[input.dataset.key] = readField(input, path + "." + input.dataset.key);
            });
            if (group.dataset.rows === "seasons") {
                entry.episodes = readRows(rowGroup(row, "episodes"), path + ".episodes");
            }
            return entry;
        });
    }

    const rootInputs = form.querySelectorAll("[data-root]");
    rootInputs.forEach(input => {
        const value = initial[input.name];
        if (value != null) input.value = Array.isArray(value) ? value.join(", ") : value;
    });
    ["cast", "sources", "seasons"].forEach(key => {
        (initial[key] ?? []).forEach(entry => addRow(rowGroup(form, key), entry));
    });
    showType();
    type.addEventListener("change", showType);
    form.addEventListener("click", event => {
        const button = event.target.closest("button[data-add], button[data-remove]");
        if (!button) return;
        const section = button.closest("fieldset");
        if (button.hasAttribute("data-remove")) {
            const parent = section.parentElement;
            section.remove();
            parent.parentElement.querySelector(":scope > button[data-add]")?.focus();
        } else {
            const row = addRow(rowGroup(section, button.dataset.add));
            row.querySelector("input:not([type=hidden])")?.focus();
        }
    });
    save.disabled = false;
    form.addEventListener("submit", async event => {
        event.preventDefault();
        save.disabled = true;
        feedback.textContent = "";
        form.querySelectorAll('[aria-invalid="true"]').forEach(field => field.removeAttribute("aria-invalid"));
        fields.clear();
        const payload = {};
        rootInputs.forEach(input => {
            if (!input.closest("fieldset[disabled]")) payload[input.name] = readField(input, input.name);
        });
        const groupKeys = { movie: ["cast"], documentary: ["sources"], series: ["seasons"] };
        groupKeys[type.value].forEach(key => payload[key] = readRows(rowGroup(form, key), key));
        const headers = requestHeaders();
        headers["Content-Type"] = "application/json";
        const revision = form.dataset.revision;
        if (revision) headers["If-Match"] = '"' + revision + '"';

        try {
            const response = await fetch(form.dataset.url, {
                method: revision ? "PUT" : "POST", headers, body: JSON.stringify(payload)
            });
            if (response.status === 401 || response.redirected) {
                throw new Error("La sesión terminó. Inicia sesión en otra pestaña y vuelve a guardar.");
            }
            if (response.status === 403) {
                throw new Error("La sesión o el token expiró. Conserva los datos y vuelve a abrir el formulario.");
            }
            if (!response.ok) {
                const error = await response.json().catch(() => ({}));
                throw new Error(error.message || "No se pudo guardar el contenido.");
            }
            window.location.assign(form.dataset.return);
        } catch (error) {
            feedback.textContent = error instanceof TypeError
                ? "No se pudo conectar al catálogo. Vuelve a intentar." : error.message;
            const path = /^Campo inválido: (.+)$/.exec(feedback.textContent)?.[1];
            const field = fields.get(path) ?? fields.get(path?.split("[")[0]);
            if (field) {
                field.setAttribute("aria-invalid", "true");
                field.setAttribute("aria-describedby", feedback.id);
                field.focus();
            } else {
                feedback.focus();
            }
        } finally {
            save.disabled = false;
        }
    });
}
