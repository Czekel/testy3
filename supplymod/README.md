# Supply Drop Mod (Fabric, MC 1.21.1)

Mod dodaje:

1. **Zrzuty zaopatrzenia** - co dzien w grze 20% szansy na zrzut. Losowe
   koordynaty, komunikat + boss bar u gory ekranu ze wspolrzednymi. Zrzut
   (wyglada jak lecaca beczka) leci 5 minut, a w momencie ladowania
   **odpycha kazdego gracza w promieniu 5 blokow** (jak mala fala
   uderzeniowa). Potem czeka zapieczetowany kolejne 5 minut, po czym
   otwiera sie jako **beczka** z lootem.
2. **Ksiega Ulaskawienia** - dropi ze zrzutu (1% szans). Nakladasz ja na
   dowolny przedmiot w **kowadle** (lewy slot: przedmiot, prawy slot:
   ksiega). Oznaczony przedmiot NIE wypada z ekwipunku po smierci - wraca do
   ciebie po respawnie, ale efekt zuzywa sie jednorazowo.
3. **System dodatkowych serc** - przedmiot "Serce" (10% ze zrzutu) po
   zjedzeniu/uzyciu podnosi twoje maksymalne zdrowie o 1 serce, az do limitu
   20 serc (40 HP). Uzyciu towarzyszy taki sam blysk na ekranie i dzwiek,
   jak przy zuzyciu Totemu Niesmiertelnosci.

## Loot w zrzucie (kazdy wpis losowany NIEZALEZNIE, moze wypasc kilka razy)

| Przedmiot | Szansa |
|---|---|
| Blok diamentu | 30% |
| Totem Niesmiertelnosci | 5% |
| Zlote jablko zaklete (Notch Apple) | 10% |
| Zlote jablko (zwykle) | 39% |
| Brodawki nether | 25% |
| Losowa czesc diamentowej zbroi | 40% |
| Zelazo | 70% |
| Zloto | 70% |
| Zaczarowana ksiazka (losowa) | 30% |
| Serce | 10% |
| Ksiega Ulaskawienia | 1% |
| Netherite scrap | 5% |
| Diamenty (luzem) | 30% |
| Miedz | 80% |
| Chleb (wypelniacz) | 60% |
| Strzaly (wypelniacz) | 50% |
| Proch strzelniczy (wypelniacz) | 35% |
| Sznurek (wypelniacz) | 40% |
| Lapis lazuli (wypelniacz) | 25% |
| Szmaragd (wypelniacz) | 15% |
| Perla Endermana (wypelniacz) | 12% |
| Siodlo (wypelniacz) | 8% |

## WAZNE - zanim zaczniesz

To jest kompletny szkielet projektu napisany recznie (bez lokalnego
srodowiska Gradle/Minecraft do kompilacji i testow - nie mam tutaj dostepu
do internetu ani Javy z pelnym MC). Zanim uruchomisz mod w grze, musisz:

1. Otworzyc projekt w **IntelliJ IDEA** z zainstalowanym pluginem
   Fabric/Gradle (albo samym Gradle) i dac mu pobrac zaleznosci
   (`./gradlew genSources` / `./gradlew build`).
2. Sprawdzic, czy nazwy metod w mixinach (`AnvilScreenHandlerMixin`,
   `PlayerDropInventoryMixin`) zgadzaja sie z mapowaniami Yarn dla
   1.21.1 - mapowania czasem drobno sie zmieniaja miedzy buildami, wiec
   jesli Gradle zglosi blad w mixinie, otworz zdekompilowane zrodla
   (`AnvilScreenHandler` / `PlayerEntity`) i popraw nazwy pol/metod.
3. Tekstury Ksiegi Ulaskawienia i Serca sa juz dolaczone (proste
   pixel-artowe 16x16, wygenerowane recznie: zwykla brazowa ksiazka jak
   z vanilla Minecrafta oraz czerwony barwnik). Podmien je
   swobodnie na wlasne w:
   - `src/main/resources/assets/supplymod/textures/item/book_of_pardon.png`
   - `src/main/resources/assets/supplymod/textures/item/heart.png`
4. Uzupelnic `src/main/resources/assets/supplymod/icon.png` (128x128) albo
   usunac linijke `"icon"` z `fabric.mod.json`.

## Uruchomienie w grze (dev)

```
./gradlew runClient
```

## Wrzucenie na GitHub

Ja (Claude) nie mam tutaj dostepu do internetu, wiec nie moge sam wypchnac
kodu na GitHuba - musisz zrobic to lokalnie, u siebie:

```bash
cd supplymod
git init
git add .
git commit -m "Supply Drop Mod - initial version"
git branch -M main
git remote add origin https://github.com/TWOJ_NICK/supplymod.git
git push -u origin main
```

(Wczesniej stworz puste repo na github.com -> "New repository", bez
README/licencji, zeby uniknac konfliktow przy pierwszym pushu).

## Znane uproszczenia / TODO

- Zrzut wizualnie to niewidzialny "armor stand" z blokiem beczki na
  glowie (dziala bez potrzeby rejestrowania wlasnej encji/renderera).
  Mozna to podmienic na wlasna encje z lepsza animacja/modelem.
- Odepchniecie graczy dziala przez predkosc (velocity), nie teleport -
  dzieki temu gracz nie przechodzi przez sciany/teren. Sila jest
  dobrana tak, by na otwartym terenie odrzucic ~5 blokow, ale realny
  dystans zalezy od terenu (schody, woda, przeszkody) - mozna dostroic
  stala `KNOCKBACK_STRENGTH` w `SupplyDropManager.java`.
- Recipe na kowadle nie ma custom modelu/nazwy w GUI poza standardowym
  zachowaniem anvil - dziala funkcjonalnie, ale nie wyswietla specjalnego
  komunikatu w interfejsie.
- Brak datageneracji lootu przez `loot_table` JSON - loot jest generowany
  w kodzie (`SupplyLoot.java`), co ulatwia obsluge "wiele wystapien tego
  samego itemu", ale oznacza, ze nie zobaczysz go w standardowym
  edytorze loot tables.

