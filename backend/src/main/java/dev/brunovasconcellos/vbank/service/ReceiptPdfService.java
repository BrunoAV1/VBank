package dev.brunovasconcellos.vbank.service;

import dev.brunovasconcellos.vbank.domain.Transfer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReceiptPdfService {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            .withZone(ZoneId.of("America/Sao_Paulo"));

    public byte[] create(Transfer transfer) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                write(content, bold, 20, 54, 785, "VBank Sandbox");
                write(content, regular, 10, 54, 764, "Comprovante de transferencia sandbox - sem valor financeiro real");
                content.moveTo(54, 746); content.lineTo(541, 746); content.stroke();
                List<String> lines = List.of(
                        "Status: " + transfer.getStatus(),
                        "Valor ficticio: R$ " + transfer.getAmount().toPlainString().replace('.', ','),
                        "Pagador: " + transfer.getSourceAccount().getUser().getFullName(),
                        "Destinatario: " + transfer.getDestinationAccount().getUser().getFullName(),
                        "Chave interna utilizada: " + transfer.getKeyUsed(),
                        "Data: " + DATE.format(transfer.getCompletedAt()),
                        "ID publico: " + transfer.getPublicId(),
                        "End-to-End ID ficticio: " + transfer.getEndToEndId(),
                        "Descricao: " + (transfer.getDescription() == null ? "Sem descricao" : transfer.getDescription())
                );
                float y = 710;
                for (String line : lines) { write(content, regular, 11, 54, y, line); y -= 30; }
                write(content, bold, 10, 54, 380, "AMBIENTE FICTICIO");
                write(content, regular, 9, 54, 360, "Nenhum valor ou transferencia desta plataforma possui valor financeiro real.");
            }
            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível gerar o comprovante PDF.", exception);
        }
    }

    private void write(PDPageContentStream content, PDType1Font font, float size, float x, float y, String text) throws IOException {
        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText(DomainNormalizer.ascii(text).replaceAll("[^\\x20-\\x7E]", "?"));
        content.endText();
    }
}
