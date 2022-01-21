#!/bin/bash
BWHT="\033[1;37m"
REG="(tput sgr0)"
LIB=../../lib/*:../
CLI=client.ClientMain

registrationclients=(
    'java -cp '$LIB' '$CLI' "reward notification|register u1 pas1 politica"'
    'java -cp '$LIB' '$CLI' "reward notification|register u2 pas2 calcio"'
    'java -cp '$LIB' '$CLI' "reward notification|register u3 pas3 arte"'
    'java -cp '$LIB' '$CLI' "reward notification|register u4 pas4 coccole"'
    'java -cp '$LIB' '$CLI' "reward notification|register u5 pas5 arte coccole"'
    )

clients=(
    'java -cp '$LIB' '$CLI' "reward notification|login mario draghi|show feed|blog|follow moody|unfollow moody|logout"'
    'java -cp '$LIB' '$CLI' "reward notification|login moody 412|show feed|blog|follow franco|unfollow franco|wallet|logout"'
    'java -cp '$LIB' '$CLI' "reward notification|login u1 pas1|show post 0|show feed|wallet btc|follow moody|logout"'
    'java -cp '$LIB' '$CLI' "reward notification|login u2 pas2|post "TITOLO POST DI U2" "CHEMUSICA"|blog|wallet btc|follow moody|logout"'
    'java -cp '$LIB' '$CLI' "reward notification|login u3 pas3|post "TITOLO POST DI U3" "BOO"|blog|wallet btc|follow mario"'
    'java -cp '$LIB' '$CLI' "reward notification|login u4 pas4|rate 1 1|follow u2|rate 1 -1|follow u4|logout"'
    'java -cp '$LIB' '$CLI' "reward notification|login u5 pas5|rate 2 1|follow u3|comment 26 "COMMENTONE"|show feed|logout"'
    )

while true 
do
    i=$(( RANDOM % ${#clients[@]}))
    echo ${clients[i]} $BASHPID
    ${clients[i]}
done

# i=$(( RANDOM % ${#clients[@]}))
# echo ${clients[1]}
# ${clients[1]}