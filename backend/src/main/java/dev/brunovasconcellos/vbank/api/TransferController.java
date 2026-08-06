package dev.brunovasconcellos.vbank.api;

import dev.brunovasconcellos.vbank.security.CurrentUser;
import dev.brunovasconcellos.vbank.service.ReceiptPdfService;
import dev.brunovasconcellos.vbank.service.TransferExecutor;
import dev.brunovasconcellos.vbank.service.TransferService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/pix/transfers")
public class TransferController {
    private final TransferService service;
    private final TransferExecutor executor;
    private final ReceiptPdfService pdfService;

    public TransferController(TransferService service, TransferExecutor executor, ReceiptPdfService pdfService) {
        this.service = service;
        this.executor = executor;
        this.pdfService = pdfService;
    }

    @PostMapping
    ApiDtos.TransferResponse transfer(Authentication auth,
                                       @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
                                       @Valid @RequestBody ApiDtos.TransferRequest request) {
        return service.transfer(CurrentUser.id(auth), request, idempotencyKey);
    }

    @GetMapping
    Page<ApiDtos.TransferResponse> list(Authentication auth,
                                        @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        return executor.list(CurrentUser.id(auth), pageable);
    }

    @GetMapping("/{id}")
    ApiDtos.TransferResponse get(Authentication auth, @PathVariable UUID id) {
        return executor.getForUser(CurrentUser.id(auth), id);
    }

    @GetMapping("/{id}/receipt")
    ApiDtos.TransferResponse receipt(Authentication auth, @PathVariable UUID id) {
        return executor.getForUser(CurrentUser.id(auth), id);
    }

    @GetMapping(value = "/{id}/receipt.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    ResponseEntity<byte[]> pdf(Authentication auth, @PathVariable UUID id) {
        var transfer = executor.getEntityForUser(CurrentUser.id(auth), id);
        byte[] bytes = pdfService.create(transfer);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("comprovante-" + transfer.getPublicId() + ".pdf", StandardCharsets.UTF_8).build();
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.APPLICATION_PDF).body(bytes);
    }
}

