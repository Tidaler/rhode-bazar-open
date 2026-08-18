package com.rhodebazar.backend.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.rhodebazar.backend.web.dto.LigneVenteDto;
import com.rhodebazar.backend.web.dto.VenteDto;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Genere un ticket de caisse (format imprimante thermique 80mm) a partir d'une vente
 * existante. Aucune donnee n'est persistee : le PDF est reconstruit a la demande depuis
 * les donnees deja enregistrees de la vente.
 *
 * L'alignement (centrage, colonnes montant a droite) s'appuie sur les mecanismes natifs
 * d'OpenPDF (Element.ALIGN_CENTER, PdfPTable a largeur fixe) plutot que sur un remplissage
 * manuel par espaces : ce dernier ne reste juste que si toutes les lignes partagent
 * exactement la meme taille de police, ce qui n'est pas le cas ici (titre plus grand).
 */
@org.springframework.stereotype.Service
public class FactureService {

    private static final float LARGEUR_TICKET = 226.77f; // 80mm
    private static final float MARGE = 12f;
    private static final float LARGEUR_UTILE = LARGEUR_TICKET - 2 * MARGE;
    private static final float TAILLE_POLICE = 8f;
    private static final float INTERLIGNE = 11f;
    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final Font policeTitre = FontFactory.getFont(FontFactory.COURIER_BOLD, TAILLE_POLICE + 3);
    private final Font policeNormale = FontFactory.getFont(FontFactory.COURIER, TAILLE_POLICE);
    private final Font policeGrasse = FontFactory.getFont(FontFactory.COURIER_BOLD, TAILLE_POLICE);

    public byte[] genererFacture(VenteDto vente) {
        List<Element> elements = construireElements(vente);

        // Hauteur estimee avec une marge de securite genereuse : un ticket qui deborde
        // sur une seconde page serait inutilisable a l'impression.
        float hauteurEstimee = MARGE * 2 + elements.size() * (INTERLIGNE + 2f) + 40f;
        Rectangle taillePage = new Rectangle(LARGEUR_TICKET, hauteurEstimee);
        Document document = new Document(taillePage, MARGE, MARGE, MARGE, MARGE);
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, sortie);
            document.open();
            for (Element element : elements) {
                document.add(element);
            }
            document.close();
        } catch (com.lowagie.text.DocumentException e) {
            throw new IllegalStateException("Erreur lors de la generation du ticket de caisse PDF", e);
        }
        return sortie.toByteArray();
    }

    private List<Element> construireElements(VenteDto vente) {
        List<Element> elements = new ArrayList<>();

        elements.add(centre("ManagEx", policeTitre));
        elements.add(centre("Rhode Bazar", policeNormale));
        elements.add(separateur());
        elements.add(gauche("Ticket: " + vente.numero(), policeNormale));
        elements.add(gauche("Date: " + vente.dateVente().format(FORMAT_DATE), policeNormale));
        elements.add(gauche("Depot: " + vente.depotNom(), policeNormale));
        if (vente.utilisateurNom() != null) {
            elements.add(gauche("Vendeur: " + vente.utilisateurNom(), policeNormale));
        }
        elements.add(gauche("Paiement: " + vente.modePaiement(), policeNormale));
        if (vente.clientNom() != null && !vente.clientNom().isBlank()) {
            elements.add(gauche("Client: " + vente.clientNom(), policeNormale));
        }
        elements.add(separateur());

        for (LigneVenteDto ligne : vente.lignes()) {
            elements.add(gauche(ligne.produitNom(), policeNormale));
            String detail = "  " + ligne.quantite() + " x " + ligne.prixUnitaire().toPlainString();
            elements.add(ligneDeuxColonnes(detail, ligne.montantLigne().toPlainString(), policeNormale));
        }
        elements.add(separateur());
        elements.add(ligneDeuxColonnes("TOTAL", vente.montantTotal().toPlainString(), policeGrasse));
        elements.add(separateur());
        elements.add(centre("Merci de votre visite", policeNormale));

        return elements;
    }

    private Paragraph centre(String texte, Font police) {
        Paragraph paragraphe = new Paragraph(INTERLIGNE, texte, police);
        paragraphe.setAlignment(Element.ALIGN_CENTER);
        return paragraphe;
    }

    private Paragraph gauche(String texte, Font police) {
        Paragraph paragraphe = new Paragraph(INTERLIGNE, texte, police);
        paragraphe.setAlignment(Element.ALIGN_LEFT);
        return paragraphe;
    }

    /**
     * Ligne pleine largeur de tirets, dont le nombre est calcule a partir de la largeur
     * reelle du caractere "-" dans la police du corps du ticket : garantit qu'elle occupe
     * toute la largeur utile quelle que soit la police, sans supposition arbitraire.
     */
    private Paragraph separateur() {
        float largeurTiret = policeNormale.getCalculatedBaseFont(false).getWidthPoint("-", policeNormale.getSize());
        int nombreTirets = Math.max(1, (int) Math.floor(LARGEUR_UTILE / largeurTiret));
        return gauche("-".repeat(nombreTirets), policeNormale);
    }

    /**
     * Ligne a deux colonnes (libelle a gauche, montant aligne a droite) via une table sans
     * bordure a largeur fixe : l'alignement reste exact quelle que soit la police utilisee,
     * contrairement a un remplissage manuel par espaces.
     */
    private PdfPTable ligneDeuxColonnes(String texteGauche, String texteDroite, Font police) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try {
            table.setTotalWidth(LARGEUR_UTILE);
            table.setLockedWidth(true);
        } catch (com.lowagie.text.DocumentException e) {
            throw new IllegalStateException("Largeur de table invalide pour le ticket", e);
        }
        table.setWidths(new float[] {6f, 4f});
        table.setSpacingBefore(0f);
        table.setSpacingAfter(0f);

        PdfPCell celluleGauche = new PdfPCell(new Paragraph(texteGauche, police));
        celluleGauche.setBorder(Rectangle.NO_BORDER);
        celluleGauche.setPadding(0f);
        celluleGauche.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(celluleGauche);

        PdfPCell celluleDroite = new PdfPCell(new Paragraph(texteDroite, police));
        celluleDroite.setBorder(Rectangle.NO_BORDER);
        celluleDroite.setPadding(0f);
        celluleDroite.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(celluleDroite);

        return table;
    }
}
