package com.aurionpro.papms.repository;

import com.aurionpro.papms.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Integer> {

    /**
     * Finds all invoices for a given organization.
     *
     * @param organizationId The ID of the organization.
     * @return A list of invoices.
     */
    List<Invoice> findByOrganizationId(Integer organizationId);

    /**
     * Finds all invoices for a given client.
     *
     * @param clientId The ID of the client.
     * @return A list of invoices.
     */
    List<Invoice> findByClientId(Integer clientId);

    /**
     * Finds a specific invoice by its number within a specific organization.
     *
     * @param invoiceNumber  The unique invoice number.
     * @param organizationId The ID of the organization that issued the invoice.
     * @return An Optional containing the invoice if found.
     */
    Optional<Invoice> findByInvoiceNumberAndOrganizationId(String invoiceNumber, Integer organizationId);
}