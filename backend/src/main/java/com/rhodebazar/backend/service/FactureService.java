package com.rhodebazar.backend.service;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.rhodebazar.backend.web.dto.LigneVenteDto;
import com.rhodebazar.backend.web.dto.VenteDto;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

/**
 * Genere une facture PDF a partir d'une vente existante. Aucune donnee n'est persistee :
 * le PDF est reconstruit a la demande depuis les donnees deja enregistrees de la vente.
 */
@org.springframework.stereotype.Service
public class FactureService {

    private static final Color VIOLET = new Color(0x5B, 0x2A, 0x86);
    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] genererFacture(VenteDto vente) {
        Document document = new Document(PageSize.A4, 40, 40, 50, 40);
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, sortie);
            document.open();

            Font policeTitre = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, VIOLET);
            Font policeSousTitre = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.DARK_GRAY);
            Font policeNormale = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.BLACK);
            Font policeEnteteTableau = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
            Font policeTotal = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, VIOLET);

            Paragraph titre = new Paragraph("ManagEx", policeTitre);
            titre.add(new Chunk(" — Rhode Bazar", policeSousTitre));
            document.add(titre);
            document.add(new Paragraph("Facture " + vente.numero(), policeSousTitre));
            document.add(Chunk.NEWLINE);

            document.add(new Paragraph("Date : " + vente.dateVente().format(FORMAT_DATE), policeNormale));
            document.add(new Paragraph("Depot : " + vente.depotNom(), policeNormale));
            document.add(new Paragraph("Mode de paiement : " + vente.modePaiement(), policeNormale));
            if (vente.clientNom() != null && !vente.clientNom().isBlank()) {
                document.add(new Paragraph("Client : " + vente.clientNom(), policeNormale));
            }
            document.add(Chunk.NEWLINE);

            PdfPTable tableau = new PdfPTable(4);
            tableau.setWidthPercentage(100);
            tableau.setWidths(new float[] {3f, 1f, 1.3f, 1.3f});

            for (String entete : new String[] {"Produit", "Quantite", "Prix unitaire", "Montant"}) {
                PdfPCell cellule = new PdfPCell(new Paragraph(entete, policeEnteteTableau));
                cellule.setBackgroundColor(VIOLET);
                cellule.setPadding(6);
                tableau.addCell(cellule);
            }

            for (LigneVenteDto ligne : vente.lignes()) {
                tableau.addCell(cellule(ligne.produitNom(), policeNormale, Element.ALIGN_LEFT));
                tableau.addCell(cellule(String.valueOf(ligne.quantite()), policeNormale, Element.ALIGN_CENTER));
                tableau.addCell(cellule(ligne.prixUnitaire().toPlainString(), policeNormale, Element.ALIGN_RIGHT));
                tableau.addCell(cellule(ligne.montantLigne().toPlainString(), policeNormale, Element.ALIGN_RIGHT));
            }
            document.add(tableau);
            document.add(Chunk.NEWLINE);

            Paragraph total = new Paragraph("Total : " + vente.montantTotal().toPlainString(), policeTotal);
            total.setAlignment(Element.ALIGN_RIGHT);
            document.add(total);

            document.add(Chunk.NEWLINE);
            document.add(Chunk.NEWLINE);
            Paragraph pied = new Paragraph("Merci de votre confiance.", policeSousTitre);
            pied.setAlignment(Element.ALIGN_CENTER);
            document.add(pied);

            document.close();
        } catch (com.lowagie.text.DocumentException e) {
            throw new IllegalStateException("Erreur lors de la generation de la facture PDF", e);
        }
        return sortie.toByteArray();
    }

    private PdfPCell cellule(String texte, Font police, int alignement) {
        PdfPCell cellule = new PdfPCell(new Paragraph(texte, police));
        cellule.setPadding(5);
        cellule.setHorizontalAlignment(alignement);
        return cellule;
    }
}
