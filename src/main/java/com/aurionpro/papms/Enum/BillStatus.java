package com.aurionpro.papms.Enum;

public enum BillStatus {
    PENDING, // Bill created, awaiting payment
    PARTIALLY_PAID, // Some amount paid, balance due
    PAID, // Fully paid
    PAY_LATER, // Marked for later payment
    INSTALLMENTS, // Being paid in installments
    OVERDUE, // Past due date, not paid
    CANCELLED // Bill cancelled
}
