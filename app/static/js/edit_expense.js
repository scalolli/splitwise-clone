function reindexSplits() {
    document.querySelectorAll('#splits-list .split-row').forEach(function(row, idx) {
        row.querySelectorAll('input, select, label').forEach(function(el) {
            if (el.name) el.name = el.name.replace(/splits-\d+-/, `splits-${idx}-`);
            if (el.id) el.id = el.id.replace(/splits-\d+-/, `splits-${idx}-`);
            if (el.htmlFor) el.htmlFor = el.htmlFor.replace(/splits-\d+-/, `splits-${idx}-`);
        });
    });
}
// Add Split
const addBtn = document.getElementById('add-split');
addBtn.addEventListener('click', function() {
    const splitsList = document.getElementById('splits-list');
    const lastRow = splitsList.querySelector('.split-row:last-child');
    const newRow = lastRow.cloneNode(true);
    // Clear values in the new row
    newRow.querySelectorAll('input').forEach(function(el) { el.value = ''; });
    newRow.querySelectorAll('select').forEach(function(el) { el.selectedIndex = 0; });
    splitsList.appendChild(newRow);
    reindexSplits();
});
// Remove Split
function updateRemoveButtons() {
    document.querySelectorAll('.remove-split').forEach(function(btn) {
        btn.onclick = function() {
            const rows = document.querySelectorAll('#splits-list .split-row');
            if (rows.length > 1) {
                btn.closest('.split-row').remove();
                reindexSplits();
            }
        };
    });
}
document.getElementById('splits-list').addEventListener('click', function(e) {
    if (e.target.classList.contains('remove-split')) {
        updateRemoveButtons();
    }
});
// Initial setup
updateRemoveButtons();

