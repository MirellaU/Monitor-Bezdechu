# -*- coding: utf-8 -*-
import numpy as np
import random

def sinc(x):
    """funkcja sinc
    potrzebna jest liczba pi (np.pi) oraz funkcja sinus (np.sin) """
    #lista zer o dlugosci x, przykladowo np.zeros(3) = [0, 0, 0]
    wynik = np.zeros(len(x))
    #obecne w x zero zamieniamy na 1
    for i in range(len(x)):
        if x[i] == 0:
            wynik[i] = 1
    #dla reszty argumentow liczymy sinc
        else:
            wynik[i] = np.sin(np.pi*x[i])/(np.pi*x[i])
    return wynik

def okno():
    """zwraca okno Hanna
    potrzebne jest zaokraglenie liczby do najblizszej calkowitej(np.ceil),
    funkcje sinus (np.sin) i cosinus (np.cos) oraz liczba pi (np.pi)"""
    fc, b  = 0.025, 0.174
    # N musi byc nieparzyste, generalnie N = np.ceil(4/b) i jesli jest parzyste
    #to dodaje 1: do tego wlasnie jest to int(np.ceil((4 / b))%2 == 0)
    N = int(np.ceil((4 / b))) + int(np.ceil((4 / b))%2 == 0)
    #przykladowo np.arange(5) = [0, 1, 2, 3, 4]
    n = np.arange(N)
    #macierz okna Hanna
    okno = 0.5*(1 - np.cos(2 * np.pi * n / (N - 1)))
    #iloczyn funkcji sinc i okna
    funkcja_sinc = sinc(2 * fc * (n - (N - 1) / 2))
    return  funkcja_sinc * okno
 
def detekcja_pikow(sygnal):
    """zwraca ile razy sredni poziom sygnalu zostal przekroczony
    w sygnale, co mozna przetlumaczyc na liczbe pikow
    potrzebna jest funkcja liczaca srednia (np.mean)"""
    srednia = np.mean(sygnal)
    piki = 0
    for i in range(len(sygnal)-1):
        if sygnal[i]<= srednia and sygnal[i+1]>srednia:
            piki += 1
    return piki

plik= open("C:/Users/mateusz.krysinski/OneDrive/studia/AE/1.txt")
#zamieniam liczby jako tekst na faktyczne liczby
#czyli "12, 15, 18, 1, 0" -> [12, 15, 18, 1, 0]
sygnal = np.array([i for i in plik][0].split(',')[:-1], dtype = int)

hann = okno()
#tutaj potrzebna jest funkcja liczaca splot (np.convolve)
splot = np.convolve(sygnal, hann)

#wygenerowanie losowej probki o dlugosci 5s
rozmiar_probki = 1000
r = random.randint(0,12000-rozmiar_probki)
probka = splot[r:r+rozmiar_probki]

liczba_pikow = detekcja_pikow(probka)
