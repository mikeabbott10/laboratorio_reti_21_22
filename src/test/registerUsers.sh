#!/bin/bash
BWHT="\033[1;37m"
REG="(tput sgr0)"
LIB=../../lib/*:../
CLI=client.ClientMain

clients=(
    'java -cp '$LIB' '$CLI' "reward notification|register u1 pas1 politica"'
    'java -cp '$LIB' '$CLI' "reward notification|register u2 pas2 teatro tag1"'
    'java -cp '$LIB' '$CLI' "reward notification|register u3 pas3 arte teatro tag1"'
    'java -cp '$LIB' '$CLI' "reward notification|register u4 pas4 sport"'
    'java -cp '$LIB' '$CLI' "reward notification|register u5 pas5 politica"'
    'java -cp '$LIB' '$CLI' "reward notification|register u6 pas6 teatro tag0"'
    'java -cp '$LIB' '$CLI' "reward notification|register u7 pas7 arte teatro tag0"'
    'java -cp '$LIB' '$CLI' "reward notification|register u8 pas8 sport"'
    'java -cp '$LIB' '$CLI' "reward notification|register u9 pas9 arte sport tag1"'
    'java -cp '$LIB' '$CLI' "reward notification|register u10 pas10 politica"'
    'java -cp '$LIB' '$CLI' "reward notification|register u11 pas11 politica"'
    'java -cp '$LIB' '$CLI' "reward notification|register u12 pas12 teatro tag0"'
    'java -cp '$LIB' '$CLI' "reward notification|register u13 pas13 arte teatro tag2"'
    'java -cp '$LIB' '$CLI' "reward notification|register u14 pas14 sport"'
    'java -cp '$LIB' '$CLI' "reward notification|register u15 pas15 politica"'
    'java -cp '$LIB' '$CLI' "reward notification|register u16 pas16 teatro tag0"'
    'java -cp '$LIB' '$CLI' "reward notification|register u17 pas17 arte tag2 tag0"'
    'java -cp '$LIB' '$CLI' "reward notification|register u18 pas18 sport"'
    'java -cp '$LIB' '$CLI' "reward notification|register u19 pas19 arte sport tag0"'
    'java -cp '$LIB' '$CLI' "reward notification|register u20 pas20 politica"'
    'java -cp '$LIB' '$CLI' "reward notification|register u21 pas21 politica"'
    'java -cp '$LIB' '$CLI' "reward notification|register u22 pas22 teatro tag0"'
    'java -cp '$LIB' '$CLI' "reward notification|register u23 pas23 arte teatro tag0"'
    'java -cp '$LIB' '$CLI' "reward notification|register u24 pas24 sport"'
    'java -cp '$LIB' '$CLI' "reward notification|register u25 pas25 politica"'
    'java -cp '$LIB' '$CLI' "reward notification|register u26 pas26 teatro tag3"'
    'java -cp '$LIB' '$CLI' "reward notification|register u27 pas27 arte teatro tag0"'
    'java -cp '$LIB' '$CLI' "reward notification|register u28 pas28 sport"'
    'java -cp '$LIB' '$CLI' "reward notification|register u29 pas29 arte sport tag3"'
    'java -cp '$LIB' '$CLI' "reward notification|register u30 pas30 politica"'
    )

i=0

until [ $i -gt ${#clients[@]} ]
do
  echo ${clients[i]} $BASHPID
  ${clients[i]}
  ((i=i+1))
done
