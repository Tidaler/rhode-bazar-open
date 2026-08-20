# -*- coding: utf-8 -*-
"""Genere l'icone .ico de l'installeur ManagEx (violet/or, un "M" stylise).
Execute une seule fois ; le resultat (installer/src/main/resources/managex.ico)
est commite dans le depot, ce script n'a pas besoin d'etre relance a chaque build.
"""
from PIL import Image, ImageDraw, ImageFont

VIOLET = (0x5B, 0x2A, 0x86, 255)
VIOLET_FONCE = (0x40, 0x1D, 0x61, 255)
OR = (0xD4, 0xAF, 0x37, 255)
BLANC = (0xFF, 0xFF, 0xFF, 255)

TAILLE = 256


def construire_image():
    image = Image.new("RGBA", (TAILLE, TAILLE), (0, 0, 0, 0))
    dessin = ImageDraw.Draw(image)

    marge = 8
    dessin.rounded_rectangle(
        [marge, marge, TAILLE - marge, TAILLE - marge],
        radius=48,
        fill=VIOLET,
        outline=VIOLET_FONCE,
        width=4,
    )

    texte = "M"
    police = None
    for nom in ("segoeuib.ttf", "arialbd.ttf", "arial.ttf"):
        try:
            police = ImageFont.truetype(nom, 148)
            break
        except OSError:
            continue
    if police is None:
        police = ImageFont.load_default()

    boite = dessin.textbbox((0, 0), texte, font=police)
    largeur_texte = boite[2] - boite[0]
    hauteur_texte = boite[3] - boite[1]
    position = ((TAILLE - largeur_texte) / 2 - boite[0], (TAILLE - hauteur_texte) / 2 - boite[1])
    dessin.text(position, texte, font=police, fill=OR)

    epaisseur = 10
    dessin.rectangle([TAILLE * 0.28, TAILLE * 0.80, TAILLE * 0.72, TAILLE * 0.80 + epaisseur], fill=OR)

    return image


def main():
    image = construire_image()
    tailles = [(256, 256), (128, 128), (64, 64), (48, 48), (32, 32), (16, 16)]
    image.save(
        r"D:\TidalEx\Projects\rhode-bazar\installer\src\main\resources\managex.ico",
        format="ICO",
        sizes=tailles,
    )
    print("Icone generee : installer/src/main/resources/managex.ico")


if __name__ == "__main__":
    main()
