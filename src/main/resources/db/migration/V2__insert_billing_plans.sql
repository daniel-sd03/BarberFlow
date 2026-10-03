-- ==========================================
-- SEED DATA: INSERÇÃO DOS PLANOS INICIAIS
-- ==========================================

INSERT INTO plans (id, code, name, price, billing_cycle)
VALUES
    ('plan-pro-monthly-001', 'PRO_MONTHLY', 'Plano PRO (Mensal)', 49.90, 'MONTHLY'),
    ('plan-pro-yearly-001', 'PRO_YEARLY', 'Plano PRO (Anual)', 499.00, 'YEARLY');