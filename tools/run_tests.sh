#!/bin/bash
set -euo pipefail

# Script de tests (presets + seeds).
# Tout est relance a chaque run (logs + resultats).

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
IP=${1:-127.0.0.1}
PORT=${2:-1337}
EQUIPE=${3:-test}
SEEDS_FILE=${4:-}
AUTO_SERVER=${AUTO_SERVER:-0}

LOG_DIR="$ROOT/IA/superAI/logs"
mkdir -p "$LOG_DIR"
rm -f "$LOG_DIR"/test*.txt
echo "Info: logs test*.txt, logs/seed-* et tools/results.csv seront ecrases."

# Compile l'IA
javac -d "$ROOT/IA" "$ROOT"/IA/superAI/*.java

run_preset() {
  local name="$1"
  shift
  echo "=== Lancement preset $name ==="
  local log_file="$LOG_DIR/test${name}.txt"
  rm -f "$log_file"
  java -cp "$ROOT/IA" superAI.ClientSuperAI "$IP" "$PORT" "$EQUIPE" log \
    "logFichier=$log_file" \
    "$@"
  if ! rg -q "^Tour " "$log_file" 2>/dev/null; then
    echo "Erreur: pas de tours ecrits pour le preset $name (serveur non joignable ?)"
    exit 1
  fi
}

start_server() {
  local seedLaby="$1"
  local seedBonus="$2"
  local seedId="${seedLaby}-${seedBonus}"
  local server_log="$LOG_DIR/server-${seedId}.log"

  if [[ ! -f "$ROOT/Serveur/PecheAuxMoulesBoucle.class" ]]; then
    (cd "$ROOT/Serveur" && javac -cp . PecheAuxMoulesBoucle.java)
  fi

  echo "=== Demarrage serveur seed ${seedId} ==="
  (
    cd "$ROOT/Serveur" || exit 1
    exec java -cp . PecheAuxMoulesBoucle \
      -nbJoueur 1 -delay 0 -timeout 3000 \
      -numLaby "$seedLaby" -numPlacementBonus "$seedBonus"
  ) > "$server_log" 2>&1 &
  SERVER_PID=$!
  wait_for_server "$server_log"
}

stop_server() {
  if [[ -n "${SERVER_PID:-}" ]]; then
    kill "$SERVER_PID" >/dev/null 2>&1 || true
    wait "$SERVER_PID" >/dev/null 2>&1 || true
    SERVER_PID=""
  fi
}

wait_for_server() {
  local server_log="$1"
  local ok=0
  for _ in $(seq 1 40); do
    if (echo > /dev/tcp/"$IP"/"$PORT") >/dev/null 2>&1; then
      ok=1
      break
    fi
    sleep 0.2
  done
  if [[ "$ok" -ne 1 ]]; then
    echo "Serveur non joignable sur ${IP}:${PORT}."
    if [[ -f "$server_log" ]]; then
      echo "Dernieres lignes du log serveur:"
      tail -n 20 "$server_log"
    fi
    stop_server
    exit 1
  fi
}

run_all_presets() {
  # Preset A (hybride equilibre)
  run_preset A \
    modeHybride=1 seuilAcceleration=12 seuilRarete=3 \
    penaliteDistance=1.4 coeffProfondeur=0.6 \
    penaliteRetour=4 penaliteBoucle=8 \
    verrouillageCible=4 seuilChangementCible=1.25 \
    toursSansPointsMax=12 gainDistanceBonusMin=2 gainDistanceBonusMinAccel=1

  # Preset B (hybride agressif/rapide)
  run_preset B \
    modeHybride=1 seuilAcceleration=8 seuilRarete=5 \
    penaliteDistance=1.8 coeffProfondeur=0.4 \
    penaliteRetour=6 penaliteBoucle=12 \
    verrouillageCible=3 seuilChangementCible=1.15 \
    toursSansPointsMax=8 gainDistanceBonusMin=1 gainDistanceBonusMinAccel=1

  # Preset C (hybride score/qualite)
  run_preset C \
    modeHybride=1 seuilAcceleration=18 seuilRarete=2 \
    valeurSaut=45 valeurTroisPas=70 \
    penaliteUtilisationSaut=6 penaliteUtilisationTroisPas=8 \
    penaliteDistance=1.2 coeffProfondeur=0.8 \
    verrouillageCible=6 seuilChangementCible=1.35 \
    toursSansPointsMax=14 gainDistanceBonusMin=3 gainDistanceBonusMinAccel=2

  # Preset D (sans hybride)
  run_preset D \
    modeHybride=0 \
    penaliteDistance=1.4 coeffProfondeur=0.6 \
    penaliteRetour=4 penaliteBoucle=8 \
    verrouillageCible=4 seuilChangementCible=1.25 \
    toursSansPointsMax=12 gainDistanceBonusMin=2 gainDistanceBonusMinAccel=1

  # Preset E (acceleration tres tot)
  run_preset E \
    modeHybride=1 seuilAcceleration=6 seuilRarete=6 \
    penaliteDistance=1.8 coeffProfondeur=0.5 \
    penaliteRetour=6 penaliteBoucle=10 \
    verrouillageCible=3 seuilChangementCible=1.2 \
    toursSansPointsMax=6 gainDistanceBonusMin=1 gainDistanceBonusMinAccel=0

  # Preset F (bonus agressif)
  run_preset F \
    modeHybride=1 seuilAcceleration=10 seuilRarete=4 \
    valeurSaut=55 valeurTroisPas=85 \
    penaliteUtilisationSaut=2 penaliteUtilisationTroisPas=3 \
    penaliteDistance=1.5 coeffProfondeur=0.5 \
    verrouillageCible=3 seuilChangementCible=1.15 \
    toursSansPointsMax=10 gainDistanceBonusMin=1 gainDistanceBonusMinAccel=0

  # Preset G (bonus parcimonieux)
  run_preset G \
    modeHybride=1 seuilAcceleration=12 seuilRarete=3 \
    valeurSaut=25 valeurTroisPas=35 \
    penaliteUtilisationSaut=10 penaliteUtilisationTroisPas=14 \
    penaliteDistance=1.6 coeffProfondeur=0.6 \
    verrouillageCible=4 seuilChangementCible=1.3 \
    toursSansPointsMax=12 gainDistanceBonusMin=3 gainDistanceBonusMinAccel=2

  # Variantes autour de G
  run_preset G1 \
    modeHybride=1 seuilAcceleration=12 seuilRarete=3 \
    valeurSaut=25 valeurTroisPas=35 \
    penaliteUtilisationSaut=10 penaliteUtilisationTroisPas=14 \
    penaliteDistance=1.6 coeffProfondeur=0.7 \
    verrouillageCible=4 seuilChangementCible=1.3 \
    toursSansPointsMax=12 gainDistanceBonusMin=3 gainDistanceBonusMinAccel=2 \
    modePlan=1 nbCiblesPlan=8 profondeurPlan=4

  run_preset G2 \
    modeHybride=1 seuilAcceleration=12 seuilRarete=3 \
    valeurSaut=25 valeurTroisPas=35 \
    penaliteUtilisationSaut=8 penaliteUtilisationTroisPas=10 \
    penaliteDistance=1.6 coeffProfondeur=0.6 \
    verrouillageCible=4 seuilChangementCible=1.3 \
    toursSansPointsMax=12 gainDistanceBonusMin=2 gainDistanceBonusMinAccel=1

  run_preset G3 \
    modeHybride=1 seuilAcceleration=10 seuilRarete=4 \
    valeurSaut=25 valeurTroisPas=35 \
    penaliteUtilisationSaut=10 penaliteUtilisationTroisPas=14 \
    penaliteDistance=1.7 coeffProfondeur=0.6 \
    verrouillageCible=3 seuilChangementCible=1.2 \
    toursSansPointsMax=10 gainDistanceBonusMin=3 gainDistanceBonusMinAccel=2

  run_preset G4 \
    modeHybride=1 seuilAcceleration=12 seuilRarete=3 \
    valeurSaut=25 valeurTroisPas=35 \
    penaliteUtilisationSaut=10 penaliteUtilisationTroisPas=14 \
    penaliteDistance=1.6 coeffProfondeur=0.6 \
    verrouillageCible=4 seuilChangementCible=1.3 \
    toursSansPointsMax=12 gainDistanceBonusMin=3 gainDistanceBonusMinAccel=2 \
    modeCompromis=1 margeCompromis=0.10 margeCompromisFin=0.03

  # Preset H (autour de B/E - penaliteDistance + accel plus tot)
  run_preset H \
    modeHybride=1 seuilAcceleration=6 seuilRarete=6 \
    penaliteDistance=2.1 coeffProfondeur=0.45 \
    penaliteRetour=6 penaliteBoucle=10 \
    verrouillageCible=2 seuilChangementCible=1.1 \
    toursSansPointsMax=6 gainDistanceBonusMin=1 gainDistanceBonusMinAccel=0

  # Preset I (autour de B/E - accel tot + bonus mini)
  run_preset I \
    modeHybride=1 seuilAcceleration=5 seuilRarete=7 \
    penaliteDistance=2.0 coeffProfondeur=0.4 \
    penaliteRetour=6 penaliteBoucle=12 \
    verrouillageCible=2 seuilChangementCible=1.1 \
    toursSansPointsMax=5 gainDistanceBonusMin=1 gainDistanceBonusMinAccel=0

  # Preset J (autour de B/E - un peu plus stable)
  run_preset J \
    modeHybride=1 seuilAcceleration=7 seuilRarete=6 \
    penaliteDistance=1.9 coeffProfondeur=0.45 \
    penaliteRetour=6 penaliteBoucle=10 \
    verrouillageCible=3 seuilChangementCible=1.15 \
    toursSansPointsMax=7 gainDistanceBonusMin=1 gainDistanceBonusMinAccel=0

  # Preset K (autour de B/E - acceleration moderee)
  run_preset K \
    modeHybride=1 seuilAcceleration=8 seuilRarete=5 \
    penaliteDistance=1.9 coeffProfondeur=0.5 \
    penaliteRetour=5 penaliteBoucle=10 \
    verrouillageCible=3 seuilChangementCible=1.15 \
    toursSansPointsMax=8 gainDistanceBonusMin=1 gainDistanceBonusMinAccel=0

  # Preset L (autour de B/E - moins penaliteDistance)
  run_preset L \
    modeHybride=1 seuilAcceleration=6 seuilRarete=6 \
    penaliteDistance=1.7 coeffProfondeur=0.45 \
    penaliteRetour=6 penaliteBoucle=10 \
    verrouillageCible=2 seuilChangementCible=1.1 \
    toursSansPointsMax=6 gainDistanceBonusMin=1 gainDistanceBonusMinAccel=0
}

if [[ -z "$SEEDS_FILE" && -f "$ROOT/tools/seeds.txt" ]]; then
  SEEDS_FILE="$ROOT/tools/seeds.txt"
fi

if [[ -n "$SEEDS_FILE" ]]; then
  RESULTS="$ROOT/tools/results.csv"
  echo "seed,preset,tours,points" > "$RESULTS"
  rm -rf "$ROOT/IA/superAI/logs/seed-"*

  while read -r seedLaby seedBonus; do
    [[ -z "$seedLaby" ]] && continue
    seedId="${seedLaby}-${seedBonus}"
    LOG_DIR="$ROOT/IA/superAI/logs/seed-${seedId}"
    mkdir -p "$LOG_DIR"
    rm -f "$LOG_DIR"/test*.txt
    echo "=== Seed ${seedId} ==="
    if [[ "$AUTO_SERVER" -eq 1 ]]; then
      start_server "$seedLaby" "$seedBonus"
    else
      echo "Lance le serveur avec : -numLaby ${seedLaby} -numPlacementBonus ${seedBonus}"
      echo "Puis appuie sur Entrée."
      read -r
    fi
    run_all_presets
    python3 "$ROOT/tools/analyse_logs.py" "$LOG_DIR"/test*.txt
    python3 "$ROOT/tools/collect_results.py" "$seedId" "$LOG_DIR"/test*.txt >> "$RESULTS"
    if [[ "$AUTO_SERVER" -eq 1 ]]; then
      stop_server
    fi
  done < "$SEEDS_FILE"

  python3 "$ROOT/tools/aggregate_results.py" "$RESULTS"
  exit 0
fi

run_all_presets
python3 "$ROOT/tools/analyse_logs.py" "$LOG_DIR"/test*.txt
