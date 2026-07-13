package com.codecore.billing.infrastructure.persistence.repository;

import com.codecore.billing.application.port.out.InvoiceQueryPort;
import com.codecore.billing.application.port.out.InvoiceRepository;
import com.codecore.billing.domain.model.invoice.Invoice;
import com.codecore.billing.domain.valueobject.InvoiceId;
import com.codecore.billing.domain.valueobject.InvoiceNumber;
import com.codecore.billing.domain.valueobject.InvoiceStatus;
import com.codecore.billing.domain.valueobject.TenantId;
import com.codecore.billing.infrastructure.persistence.entity.InvoiceEntity;
import com.codecore.billing.infrastructure.persistence.entity.InvoiceLineEntity;
import com.codecore.billing.infrastructure.persistence.mapper.InvoiceMapper;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Hexagonal adapter: implements outbound Invoice persistence ports using R2DBC.
 * The aggregate spans two tables ({@code billing.invoice} + {@code billing.invoice_line}) — every
 * save fully replaces the line set (ADR-017 §8/§9: lines are internal entities, no partial updates).
 */
@Repository
public class R2dbcInvoiceRepository implements InvoiceRepository, InvoiceQueryPort {

    private final SpringDataInvoiceRepository springDataInvoiceRepository;
    private final SpringDataInvoiceLineRepository springDataInvoiceLineRepository;
    private final InvoiceMapper invoiceMapper;

    public R2dbcInvoiceRepository(
            SpringDataInvoiceRepository springDataInvoiceRepository,
            SpringDataInvoiceLineRepository springDataInvoiceLineRepository,
            InvoiceMapper invoiceMapper
    ) {
        this.springDataInvoiceRepository = springDataInvoiceRepository;
        this.springDataInvoiceLineRepository = springDataInvoiceLineRepository;
        this.invoiceMapper = invoiceMapper;
    }

    @Override
    public Mono<Invoice> save(Invoice invoice) {
        return springDataInvoiceRepository
                .existsById(invoice.id().value())
                .flatMap(exists -> springDataInvoiceRepository.save(invoiceMapper.toEntity(invoice, !exists)))
                .flatMap(savedInvoice -> springDataInvoiceLineRepository
                        .deleteAllByInvoiceId(savedInvoice.getInvoiceId())
                        .thenMany(springDataInvoiceLineRepository.saveAll(invoiceMapper.toLineEntities(invoice)))
                        .collectList()
                        .map(lineEntities -> invoiceMapper.toDomain(savedInvoice, lineEntities)));
    }

    @Override
    public Mono<Invoice> findById(InvoiceId id) {
        return springDataInvoiceRepository.findById(id.value())
                .flatMap(this::withLines);
    }

    @Override
    public Mono<Invoice> findByIdAndTenantId(InvoiceId id, TenantId tenantId) {
        return springDataInvoiceRepository.findByInvoiceIdAndTenantId(id.value(), tenantId.value())
                .flatMap(this::withLines);
    }

    @Override
    public Mono<Boolean> existsById(InvoiceId id) {
        return springDataInvoiceRepository.existsById(id.value());
    }

    @Override
    public Mono<Boolean> existsByIdAndTenantId(InvoiceId id, TenantId tenantId) {
        return springDataInvoiceRepository.existsByInvoiceIdAndTenantId(id.value(), tenantId.value());
    }

    @Override
    public Flux<Invoice> findByTenantId(TenantId tenantId) {
        return springDataInvoiceRepository.findAllByTenantId(tenantId.value())
                .flatMap(this::withLines);
    }

    @Override
    public Flux<Invoice> findByTenantIdAndStatus(TenantId tenantId, InvoiceStatus status) {
        return springDataInvoiceRepository.findAllByTenantIdAndStatus(tenantId.value(), status.name())
                .flatMap(this::withLines);
    }

    @Override
    public Mono<Boolean> existsByTenantIdAndInvoiceNumber(TenantId tenantId, InvoiceNumber invoiceNumber) {
        return springDataInvoiceRepository.existsByTenantIdAndInvoiceNumber(
                tenantId.value(),
                invoiceNumber.value()
        );
    }

    @Override
    public Mono<Boolean> existsByTenantIdAndInvoiceNumberExcludingId(
            TenantId tenantId,
            InvoiceNumber invoiceNumber,
            InvoiceId excludeInvoiceId
    ) {
        return springDataInvoiceRepository.existsByTenantIdAndInvoiceNumberAndInvoiceIdNot(
                tenantId.value(),
                invoiceNumber.value(),
                excludeInvoiceId.value()
        );
    }

    @Override
    public Mono<Long> countByTenantId(TenantId tenantId) {
        return springDataInvoiceRepository.countByTenantId(tenantId.value());
    }

    private Mono<Invoice> withLines(InvoiceEntity entity) {
        return springDataInvoiceLineRepository.findAllByInvoiceId(entity.getInvoiceId())
                .collectList()
                .map((List<InvoiceLineEntity> lineEntities) -> invoiceMapper.toDomain(entity, lineEntities));
    }
}
