package com.aurionpro.papms.repository;

import com.aurionpro.papms.entity.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    boolean existsByAccountNumber(String accountNumber);

    Optional<BankAccount> findByEmployeeId(Long employeeId);

    List<BankAccount> findByEmployeeOrganizationId(Integer organizationId);

    @Query("SELECT ba FROM BankAccount ba WHERE ba.id = :accountId AND ba.employee.organization.id = :organizationId")
    Optional<BankAccount> findByIdAndEmployeeOrganizationId(@Param("accountId") Long accountId,
                                                            @Param("organizationId") Integer organizationId);

    @Query("SELECT ba FROM BankAccount ba WHERE ba.employee.id = :employeeId AND ba.employee.organization.id = :organizationId")
    List<BankAccount> findByEmployeeIdAndEmployeeOrganizationId(@Param("employeeId") Long employeeId,
                                                                @Param("organizationId") Integer organizationId);

    // Vendor bank account queries
    @Query("SELECT ba FROM BankAccount ba WHERE ba.vendor.id = :vendorId AND ba.vendor.organization.id = :organizationId")
    List<BankAccount> findByVendorIdAndVendorOrganizationId(@Param("vendorId") Long vendorId,
                                                            @Param("organizationId") Integer organizationId);

    @Query("SELECT ba FROM BankAccount ba WHERE ba.id = :accountId AND ba.vendor.organization.id = :organizationId")
    Optional<BankAccount> findByIdAndVendorOrganizationId(@Param("accountId") Long accountId,
                                                          @Param("organizationId") Integer organizationId);

    Optional<BankAccount> findByVendorIdAndIsPrimaryTrue(Long vendorId);

    List<BankAccount> findByVendorId(Long vendorId);


    @Query("SELECT ba FROM BankAccount ba WHERE ba.vendor.id = :vendorId AND ba.ownerType = 'VENDOR'")
    Optional<BankAccount> findPrimaryByVendorId(@Param("vendorId") Long vendorId);

    @Query("SELECT ba FROM BankAccount ba WHERE ba.ownerType = 'VENDOR' AND ba.vendor.organization.id = :organizationId")
    List<BankAccount> findVendorAccountsByOrganizationId(@Param("organizationId") Integer organizationId);
}