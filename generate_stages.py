#!/usr/bin/env python3
"""
Generate expanded stages.json for RelationalIQ app.
6 modules, ~90 stages total, based on RFT research framework.
"""

import json
import random
import hashlib

random.seed(42)  # Reproducible

# --- Stimulus Generation ---

CONSONANTS = list("BCDFGHJKLMNPQRSTVWXYZ")
VOWELS = list("AEIOU")

_used_stimuli = set()

def gen_stim():
    """Generate a unique 4-letter CVCV or CVCC stimulus."""
    for _ in range(1000):
        c1, c2 = random.choice(CONSONANTS), random.choice(CONSONANTS)
        v1, v2 = random.choice(VOWELS), random.choice(VOWELS)
        patterns = [
            f"{c1}{v1}{c2}{random.choice(CONSONANTS)}",
            f"{c1}{v1}{c2}{v2}",
        ]
        s = random.choice(patterns)
        if s not in _used_stimuli:
            _used_stimuli.add(s)
            return s
    raise RuntimeError("Ran out of unique stimuli")

def fresh_stimuli(n):
    return [gen_stim() for _ in range(n)]


# --- Relation helpers ---

RELATION_DISPLAY = {
    "SAME": "the same as",
    "DIFFERENT": "different from",
    "OPPOSITE": "opposite to",
    "MORE_THAN": "more than",
    "LESS_THAN": "less than",
    "BEFORE": "before",
    "AFTER": "after",
    "CONTAINS": "contains",
    "WITHIN": "within",
}

RELATION_SYMBOL = {
    "SAME": "=", "DIFFERENT": "!=", "OPPOSITE": "<->",
    "MORE_THAN": ">", "LESS_THAN": "<",
    "BEFORE": "->", "AFTER": "<-",
    "CONTAINS": "D", "WITHIN": "C",
}

INVERSE = {
    "SAME": "SAME", "DIFFERENT": "DIFFERENT", "OPPOSITE": "OPPOSITE",
    "MORE_THAN": "LESS_THAN", "LESS_THAN": "MORE_THAN",
    "BEFORE": "AFTER", "AFTER": "BEFORE",
    "CONTAINS": "WITHIN", "WITHIN": "CONTAINS",
}

TRANSITIVE = {"SAME", "MORE_THAN", "LESS_THAN", "BEFORE", "AFTER", "CONTAINS", "WITHIN"}


def premise(a, rel, b):
    return {"stimulusA": a, "relationType": rel, "stimulusB": b}


def chain_explanation(stims, rel, correct_answer, probe_rel, probe_a, probe_b):
    sym = RELATION_SYMBOL.get(rel, rel)
    chain_str = f" {sym} ".join(stims)
    if correct_answer:
        return f"{chain_str}, so {probe_a} is {RELATION_DISPLAY[probe_rel]} {probe_b}."
    else:
        inv_display = RELATION_DISPLAY.get(INVERSE.get(probe_rel, probe_rel), probe_rel)
        return f"{chain_str}, so {probe_a} is {inv_display} {probe_b}, not {RELATION_DISPLAY[probe_rel]}."


# --- Trial Generators ---

def make_transitive_trial(trial_id, rel, num_premises, correct=True, time_limit=30):
    """Generate a trial for transitive relations (SAME, MORE_THAN, LESS_THAN, BEFORE, AFTER, CONTAINS, WITHIN)."""
    stims = fresh_stimuli(num_premises + 1)
    premises = [premise(stims[i], rel, stims[i+1]) for i in range(num_premises)]

    if correct:
        probe_a, probe_b = stims[0], stims[-1]
        probe_rel = rel
        explanation = chain_explanation(stims, rel, True, probe_rel, probe_a, probe_b)
    else:
        # Ask the reversed question (should be false for asymmetric relations)
        probe_a, probe_b = stims[-1], stims[0]
        probe_rel = rel
        explanation = chain_explanation(stims, rel, False, probe_rel, probe_a, probe_b)

    return {
        "id": trial_id,
        "premises": premises,
        "probeStimA": probe_a,
        "probeRelation": probe_rel,
        "probeStimB": probe_b,
        "correctAnswer": correct,
        "explanation": explanation,
        "timeLimitSeconds": time_limit
    }


def make_symmetry_trial(trial_id, rel, correct=True, time_limit=30):
    """Single premise symmetry trial: A rel B -> B rel A?"""
    a, b = fresh_stimuli(2)
    prems = [premise(a, rel, b)]
    if correct:
        return {
            "id": trial_id, "premises": prems,
            "probeStimA": b, "probeRelation": rel, "probeStimB": a,
            "correctAnswer": True,
            "explanation": f"If {a} is {RELATION_DISPLAY[rel]} {b}, then {b} is {RELATION_DISPLAY[rel]} {a} (symmetry).",
            "timeLimitSeconds": time_limit
        }
    else:
        # Ask with wrong relation
        wrong_rel = "DIFFERENT" if rel == "SAME" else "SAME"
        return {
            "id": trial_id, "premises": prems,
            "probeStimA": a, "probeRelation": wrong_rel, "probeStimB": b,
            "correctAnswer": False,
            "explanation": f"{a} is {RELATION_DISPLAY[rel]} {b}, not {RELATION_DISPLAY[wrong_rel]}.",
            "timeLimitSeconds": time_limit
        }


def make_same_wrong_rel_trial(trial_id, num_premises, time_limit=30):
    """SAME chain but ask DIFFERENT (answer: false)."""
    stims = fresh_stimuli(num_premises + 1)
    prems = [premise(stims[i], "SAME", stims[i+1]) for i in range(num_premises)]
    return {
        "id": trial_id, "premises": prems,
        "probeStimA": stims[0], "probeRelation": "DIFFERENT", "probeStimB": stims[-1],
        "correctAnswer": False,
        "explanation": f"{' = '.join(stims)}, so {stims[0]} is the same as {stims[-1]}, not different.",
        "timeLimitSeconds": time_limit
    }


def make_inverse_trial(trial_id, rel, num_premises, time_limit=30):
    """Chain of rel, then ask inverse relation in reverse order (answer: true)."""
    stims = fresh_stimuli(num_premises + 1)
    prems = [premise(stims[i], rel, stims[i+1]) for i in range(num_premises)]
    inv = INVERSE[rel]
    return {
        "id": trial_id, "premises": prems,
        "probeStimA": stims[-1], "probeRelation": inv, "probeStimB": stims[0],
        "correctAnswer": True,
        "explanation": f"{stims[0]} is {RELATION_DISPLAY[rel]} {stims[-1]}, so {stims[-1]} is {RELATION_DISPLAY[inv]} {stims[0]}.",
        "timeLimitSeconds": time_limit
    }


def make_opposite_chain_trial(trial_id, num_premises, correct=True, time_limit=30):
    """
    Chain of OPPOSITE relations.
    Even number of OPPOSITEs = SAME, odd = OPPOSITE.
    """
    stims = fresh_stimuli(num_premises + 1)
    prems = [premise(stims[i], "OPPOSITE", stims[i+1]) for i in range(num_premises)]

    derived_is_same = (num_premises % 2 == 0)

    if correct:
        if derived_is_same:
            probe_rel = "SAME"
        else:
            probe_rel = "OPPOSITE"
        return {
            "id": trial_id, "premises": prems,
            "probeStimA": stims[0], "probeRelation": probe_rel, "probeStimB": stims[-1],
            "correctAnswer": True,
            "explanation": f"{'Opposite ' * num_premises}applied {num_premises} time(s): {stims[0]} is {RELATION_DISPLAY[probe_rel]} {stims[-1]}.",
            "timeLimitSeconds": time_limit
        }
    else:
        if derived_is_same:
            probe_rel = "OPPOSITE"
        else:
            probe_rel = "SAME"
        return {
            "id": trial_id, "premises": prems,
            "probeStimA": stims[0], "probeRelation": probe_rel, "probeStimB": stims[-1],
            "correctAnswer": False,
            "explanation": f"With {num_premises} opposite relation(s), {stims[0]} is {'the same as' if derived_is_same else 'opposite to'} {stims[-1]}, not {RELATION_DISPLAY[probe_rel]}.",
            "timeLimitSeconds": time_limit
        }


def make_mixed_same_rel_trial(trial_id, rel, num_premises, time_limit=30):
    """SAME + transitive rel chain: A=B, B rel C -> A rel C."""
    stims = fresh_stimuli(num_premises + 1)
    prems = [premise(stims[0], "SAME", stims[1])]
    for i in range(1, num_premises):
        prems.append(premise(stims[i], rel, stims[i+1]))

    return {
        "id": trial_id, "premises": prems,
        "probeStimA": stims[0], "probeRelation": rel, "probeStimB": stims[-1],
        "correctAnswer": True,
        "explanation": f"{stims[0]} = {stims[1]} and {stims[1]} is {RELATION_DISPLAY[rel]} {stims[-1]}, so {stims[0]} is {RELATION_DISPLAY[rel]} {stims[-1]}.",
        "timeLimitSeconds": time_limit
    }


def make_same_opposite_trial(trial_id, num_same, time_limit=30):
    """A=B, B opposite C -> A opposite C."""
    a, b, c = fresh_stimuli(3)
    prems = [premise(a, "SAME", b), premise(b, "OPPOSITE", c)]
    return {
        "id": trial_id, "premises": prems,
        "probeStimA": a, "probeRelation": "OPPOSITE", "probeStimB": c,
        "correctAnswer": True,
        "explanation": f"{a} = {b} and {b} is opposite to {c}, so {a} is opposite to {c}.",
        "timeLimitSeconds": time_limit
    }


def make_containment_trial(trial_id, num_premises, correct=True, time_limit=30):
    """Containment chain: Box A contains Box B, Box B contains C -> A contains C."""
    stims = fresh_stimuli(num_premises + 1)
    prems = [premise(stims[i], "CONTAINS", stims[i+1]) for i in range(num_premises)]

    if correct:
        return {
            "id": trial_id, "premises": prems,
            "probeStimA": stims[0], "probeRelation": "CONTAINS", "probeStimB": stims[-1],
            "correctAnswer": True,
            "explanation": f"Transitive containment: {stims[0]} contains {stims[-1]}.",
            "timeLimitSeconds": time_limit
        }
    else:
        return {
            "id": trial_id, "premises": prems,
            "probeStimA": stims[-1], "probeRelation": "CONTAINS", "probeStimB": stims[0],
            "correctAnswer": False,
            "explanation": f"{stims[0]} contains {stims[-1]}, not the other way around.",
            "timeLimitSeconds": time_limit
        }


def make_mixed_frame_trial(trial_id, rels, time_limit=30):
    """
    Mixed frame trial using a sequence of relation types.
    rels: list of relation types for each premise in the chain.
    """
    n = len(rels)
    stims = fresh_stimuli(n + 1)
    prems = [premise(stims[i], rels[i], stims[i+1]) for i in range(n)]

    # Derive the relationship between first and last stimulus
    derived_rel, is_valid = derive_relation(rels)

    if is_valid and derived_rel:
        return {
            "id": trial_id, "premises": prems,
            "probeStimA": stims[0], "probeRelation": derived_rel, "probeStimB": stims[-1],
            "correctAnswer": True,
            "explanation": build_mixed_explanation(stims, rels, derived_rel, True),
            "timeLimitSeconds": time_limit
        }
    else:
        # Use a wrong relation for false trial
        wrong_rel = random.choice(["SAME", "MORE_THAN", "LESS_THAN"])
        return {
            "id": trial_id, "premises": prems,
            "probeStimA": stims[0], "probeRelation": wrong_rel, "probeStimB": stims[-1],
            "correctAnswer": False,
            "explanation": build_mixed_explanation(stims, rels, wrong_rel, False),
            "timeLimitSeconds": time_limit
        }


def derive_relation(rels):
    """
    Derive the composite relation from a chain of relations.
    Returns (relation, is_valid) tuple.
    """
    # Simplify: track direction and type
    current = "IDENTITY"  # Start as identity

    for rel in rels:
        if current == "IDENTITY":
            current = rel
        elif rel == "SAME":
            pass  # SAME doesn't change the current relation
        elif current == "SAME":
            current = rel
        elif rel == "OPPOSITE":
            if current == "OPPOSITE":
                current = "SAME"
            elif current in ("MORE_THAN", "LESS_THAN"):
                current = INVERSE[current]
            else:
                current = "OPPOSITE"
        elif current == "OPPOSITE":
            if rel in ("MORE_THAN", "LESS_THAN"):
                current = INVERSE[rel]
            else:
                return None, False
        elif current == rel:
            pass  # Same transitive relation continues
        elif rel == INVERSE.get(current):
            return None, False  # Contradictory
        else:
            return None, False  # Incompatible frame types

    if current == "IDENTITY":
        current = "SAME"
    return current, True


def build_mixed_explanation(stims, rels, probe_rel, correct):
    """Build explanation for mixed-frame trial."""
    parts = []
    for i, rel in enumerate(rels):
        parts.append(f"{stims[i]} is {RELATION_DISPLAY[rel]} {stims[i+1]}")
    chain_desc = ", ".join(parts)
    if correct:
        return f"{chain_desc}. Therefore {stims[0]} is {RELATION_DISPLAY[probe_rel]} {stims[-1]}."
    else:
        return f"{chain_desc}. Therefore {stims[0]} is NOT {RELATION_DISPLAY[probe_rel]} {stims[-1]}."


# --- Stage Definitions ---

def make_stage(stage_id, title, description, module, frame_type, sub_frame,
               relation_types, premise_count, derivation_depth, difficulty,
               mastery_threshold, time_limit, xp_reward, est_time,
               training_trials, test_trials):
    return {
        "id": stage_id,
        "title": title,
        "description": description,
        "module": module,
        "frame_type": frame_type,
        "sub_frame": sub_frame,
        "relationTypes": relation_types,
        "premiseCount": premise_count,
        "derivation_depth": derivation_depth,
        "difficulty": difficulty,
        "masteryThreshold": mastery_threshold,
        "timeLimitSeconds": time_limit,
        "xpReward": xp_reward,
        "estimated_time_minutes": est_time,
        "trainingTrials": training_trials,
        "testTrials": test_trials
    }


def gen_id(stage_id, block, trial_num):
    """Generate trial ID: s{stageId}_{block}{trialNum}"""
    return f"s{stage_id}_{block}{trial_num}"


# ============================================================
# MODULE 1: COORDINATION (Same/Different) - Stages 1-16
# ============================================================

def generate_m1_stages():
    stages = []
    sid = 1

    # --- Stages 1-3: BEGINNER - Single premise symmetry ---
    for i in range(3):
        training = []
        test = []
        tl = 30
        for t in range(5):
            rel = "SAME" if t % 2 == 0 else "DIFFERENT"
            correct = t % 3 != 2
            training.append(make_symmetry_trial(gen_id(sid, "tr", t+1), rel, correct, tl))
        for t in range(3):
            rel = "SAME" if t % 2 == 0 else "DIFFERENT"
            correct = t != 1
            test.append(make_symmetry_trial(gen_id(sid, "te", t+1), rel, correct, tl))

        stages.append(make_stage(
            sid, f"Coordination Basics {i+1}",
            "Learn Same and Different relations with single-premise symmetry trials.",
            "M1_Coordination", "COORDINATION", "SAME_DIFFERENT",
            ["SAME", "DIFFERENT"], 1, 1, "BEGINNER",
            0.80, tl, 80 + i*10, 3, training, test
        ))
        sid += 1

    # --- Stages 4-7: EASY - Two premise transitivity ---
    for i in range(4):
        training, test = [], []
        tl = 28
        for t in range(5):
            if t < 3:
                training.append(make_transitive_trial(gen_id(sid, "tr", t+1), "SAME", 2, True, tl))
            elif t == 3:
                training.append(make_same_wrong_rel_trial(gen_id(sid, "tr", t+1), 2, tl))
            else:
                training.append(make_transitive_trial(gen_id(sid, "tr", t+1), "SAME", 2, False, tl))
        for t in range(3):
            correct = t != 1
            training_trial = make_transitive_trial(gen_id(sid, "te", t+1), "SAME", 2, correct, tl)
            test.append(training_trial)

        stages.append(make_stage(
            sid, f"Derived Coordination {i+1}",
            "Derive Same relations across two-premise chains using transitivity.",
            "M1_Coordination", "COORDINATION", "SAME",
            ["SAME", "DIFFERENT"], 2, 2, "EASY",
            0.80, tl, 100 + i*10, 3, training, test
        ))
        sid += 1

    # --- Stages 8-11: MEDIUM - Two premise mixed SAME/DIFFERENT ---
    for i in range(4):
        training, test = [], []
        tl = 25
        for t in range(5):
            if t % 2 == 0:
                training.append(make_transitive_trial(gen_id(sid, "tr", t+1), "SAME", 2, True, tl))
            else:
                training.append(make_same_wrong_rel_trial(gen_id(sid, "tr", t+1), 2, tl))
        for t in range(3):
            if t == 0:
                test.append(make_transitive_trial(gen_id(sid, "te", t+1), "SAME", 2, True, tl))
            elif t == 1:
                test.append(make_same_wrong_rel_trial(gen_id(sid, "te", t+1), 2, tl))
            else:
                test.append(make_transitive_trial(gen_id(sid, "te", t+1), "SAME", 2, False, tl))

        stages.append(make_stage(
            sid, f"Coordination Fluency {i+1}",
            "Build speed and accuracy with Same/Different chains under time pressure.",
            "M1_Coordination", "COORDINATION", "SAME_DIFFERENT",
            ["SAME", "DIFFERENT"], 2, 2, "MEDIUM",
            0.85, tl, 120 + i*10, 4, training, test
        ))
        sid += 1

    # --- Stages 12-14: HARD - Three premise chains ---
    for i in range(3):
        training, test = [], []
        tl = 30
        for t in range(5):
            correct = t % 2 == 0
            training.append(make_transitive_trial(gen_id(sid, "tr", t+1), "SAME", 3, correct, tl))
        for t in range(3):
            correct = t != 1
            test.append(make_transitive_trial(gen_id(sid, "te", t+1), "SAME", 3, correct, tl))

        stages.append(make_stage(
            sid, f"Long Coordination Chains {i+1}",
            "Derive Same relations across three-premise chains.",
            "M1_Coordination", "COORDINATION", "SAME",
            ["SAME", "DIFFERENT"], 3, 3, "HARD",
            0.85, tl, 150 + i*10, 4, training, test
        ))
        sid += 1

    # --- Stages 15-16: HARD - Four premise chains ---
    for i in range(2):
        training, test = [], []
        tl = 35
        for t in range(5):
            correct = t % 2 == 0
            training.append(make_transitive_trial(gen_id(sid, "tr", t+1), "SAME", 4, correct, tl))
        for t in range(3):
            correct = t != 1
            test.append(make_transitive_trial(gen_id(sid, "te", t+1), "SAME", 4, correct, tl))

        stages.append(make_stage(
            sid, f"Extended Coordination {i+1}",
            "Master four-premise Same relation chains requiring deep derivation.",
            "M1_Coordination", "COORDINATION", "SAME",
            ["SAME", "DIFFERENT"], 4, 4, "HARD",
            0.85, tl, 170 + i*10, 5, training, test
        ))
        sid += 1

    return stages, sid


# ============================================================
# MODULE 2: COMPARISON (More/Less) - Stages 17-36
# ============================================================

def generate_m2_stages(start_id):
    stages = []
    sid = start_id

    # --- Stages 17-19: BEGINNER - Single premise basic ---
    for i in range(3):
        training, test = [], []
        tl = 30
        for t in range(5):
            rel = "MORE_THAN" if t % 2 == 0 else "LESS_THAN"
            training.append(make_symmetry_trial(gen_id(sid, "tr", t+1), rel, t % 3 != 2, tl))
        for t in range(3):
            rel = "MORE_THAN" if t % 2 == 0 else "LESS_THAN"
            test.append(make_symmetry_trial(gen_id(sid, "te", t+1), rel, t != 1, tl))

        stages.append(make_stage(
            sid, f"Comparison Basics {i+1}",
            "Learn More Than and Less Than relations with basic trials.",
            "M2_Comparison", "COMPARISON", "MORE_LESS",
            ["MORE_THAN", "LESS_THAN"], 1, 1, "BEGINNER",
            0.80, tl, 80 + i*10, 3, training, test
        ))
        sid += 1

    # --- Stages 20-24: EASY - Two premise transitivity ---
    for i in range(5):
        training, test = [], []
        tl = 28
        rel = "MORE_THAN" if i % 2 == 0 else "LESS_THAN"
        for t in range(5):
            if t < 2:
                training.append(make_transitive_trial(gen_id(sid, "tr", t+1), rel, 2, True, tl))
            elif t == 2:
                training.append(make_inverse_trial(gen_id(sid, "tr", t+1), rel, 2, tl))
            else:
                training.append(make_transitive_trial(gen_id(sid, "tr", t+1), rel, 2, False, tl))
        for t in range(3):
            test.append(make_transitive_trial(gen_id(sid, "te", t+1), rel, 2, t != 1, tl))

        stages.append(make_stage(
            sid, f"Transitive Comparison {i+1}",
            f"Derive {'More Than' if rel == 'MORE_THAN' else 'Less Than'} relations across two-premise chains.",
            "M2_Comparison", "COMPARISON", "MORE_LESS",
            ["MORE_THAN", "LESS_THAN"], 2, 2, "EASY",
            0.80, tl, 100 + i*10, 3, training, test
        ))
        sid += 1

    # --- Stages 25-29: MEDIUM - Two premise with mixed MORE/LESS and inverse ---
    for i in range(5):
        training, test = [], []
        tl = 25
        for t in range(5):
            rel = random.choice(["MORE_THAN", "LESS_THAN"])
            if t % 3 == 0:
                training.append(make_transitive_trial(gen_id(sid, "tr", t+1), rel, 2, True, tl))
            elif t % 3 == 1:
                training.append(make_inverse_trial(gen_id(sid, "tr", t+1), rel, 2, tl))
            else:
                training.append(make_transitive_trial(gen_id(sid, "tr", t+1), rel, 2, False, tl))
        for t in range(3):
            rel = "MORE_THAN" if t % 2 == 0 else "LESS_THAN"
            test.append(make_transitive_trial(gen_id(sid, "te", t+1), rel, 2, t != 2, tl))

        stages.append(make_stage(
            sid, f"Comparison Fluency {i+1}",
            "Build speed with mixed More/Less chains and inverse derivations.",
            "M2_Comparison", "COMPARISON", "MORE_LESS",
            ["MORE_THAN", "LESS_THAN"], 2, 2, "MEDIUM",
            0.85, tl, 120 + i*10, 4, training, test
        ))
        sid += 1

    # --- Stages 30-33: HARD - Three premise chains ---
    for i in range(4):
        training, test = [], []
        tl = 30
        rel = "MORE_THAN" if i % 2 == 0 else "LESS_THAN"
        for t in range(5):
            if t < 2:
                training.append(make_transitive_trial(gen_id(sid, "tr", t+1), rel, 3, True, tl))
            elif t == 2:
                training.append(make_inverse_trial(gen_id(sid, "tr", t+1), rel, 3, tl))
            else:
                training.append(make_transitive_trial(gen_id(sid, "tr", t+1), rel, 3, False, tl))
        for t in range(3):
            test.append(make_transitive_trial(gen_id(sid, "te", t+1), rel, 3, t != 1, tl))

        stages.append(make_stage(
            sid, f"Extended Comparison {i+1}",
            "Derive relations across three-item comparison chains.",
            "M2_Comparison", "COMPARISON", "MORE_LESS",
            ["MORE_THAN", "LESS_THAN"], 3, 3, "HARD",
            0.85, tl, 150 + i*10, 4, training, test
        ))
        sid += 1

    # --- Stages 34-36: ADVANCED - Four premise chains ---
    for i in range(3):
        training, test = [], []
        tl = 35
        for t in range(5):
            rel = "MORE_THAN" if t % 2 == 0 else "LESS_THAN"
            correct = t % 2 == 0
            training.append(make_transitive_trial(gen_id(sid, "tr", t+1), rel, 4, correct, tl))
        for t in range(3):
            rel = "MORE_THAN" if t % 2 == 0 else "LESS_THAN"
            test.append(make_transitive_trial(gen_id(sid, "te", t+1), rel, 4, t != 1, tl))

        stages.append(make_stage(
            sid, f"Advanced Comparison {i+1}",
            "Master four-item transitive comparison chains for deep reasoning.",
            "M2_Comparison", "COMPARISON", "MORE_LESS",
            ["MORE_THAN", "LESS_THAN"], 4, 4, "ADVANCED",
            0.90, tl, 180 + i*10, 5, training, test
        ))
        sid += 1

    return stages, sid


# ============================================================
# MODULE 3: OPPOSITION - Stages 37-50
# ============================================================

def generate_m3_stages(start_id):
    stages = []
    sid = start_id

    # --- Stages 37-39: MEDIUM - Single premise symmetry ---
    for i in range(3):
        training, test = [], []
        tl = 25
        for t in range(5):
            if t < 3:
                training.append(make_symmetry_trial(gen_id(sid, "tr", t+1), "OPPOSITE", True, tl))
            else:
                training.append(make_symmetry_trial(gen_id(sid, "tr", t+1), "OPPOSITE", False, tl))
        for t in range(3):
            test.append(make_symmetry_trial(gen_id(sid, "te", t+1), "OPPOSITE", t != 1, tl))

        stages.append(make_stage(
            sid, f"Opposition Basics {i+1}",
            "Learn that Opposite relations are symmetrical.",
            "M3_Opposition", "OPPOSITION", "OPPOSITE",
            ["OPPOSITE"], 1, 1, "MEDIUM",
            0.80, tl, 100 + i*10, 3, training, test
        ))
        sid += 1

    # --- Stages 40-43: MEDIUM - Double opposite = same ---
    for i in range(4):
        training, test = [], []
        tl = 28
        for t in range(5):
            if t < 2:
                training.append(make_opposite_chain_trial(gen_id(sid, "tr", t+1), 2, True, tl))
            elif t == 2:
                training.append(make_opposite_chain_trial(gen_id(sid, "tr", t+1), 2, False, tl))
            elif t == 3:
                training.append(make_same_opposite_trial(gen_id(sid, "tr", t+1), 1, tl))
            else:
                training.append(make_opposite_chain_trial(gen_id(sid, "tr", t+1), 1, True, tl))
        for t in range(3):
            if t == 0:
                test.append(make_opposite_chain_trial(gen_id(sid, "te", t+1), 2, True, tl))
            elif t == 1:
                test.append(make_opposite_chain_trial(gen_id(sid, "te", t+1), 2, False, tl))
            else:
                test.append(make_same_opposite_trial(gen_id(sid, "te", t+1), 1, tl))

        stages.append(make_stage(
            sid, f"Double Opposition {i+1}",
            "Discover that opposite of opposite equals same. Combine Same and Opposite frames.",
            "M3_Opposition", "OPPOSITION", "OPPOSITE",
            ["SAME", "OPPOSITE"], 2, 2, "MEDIUM",
            0.85, tl, 120 + i*10, 4, training, test
        ))
        sid += 1

    # --- Stages 44-47: HARD - Three premise opposite chains ---
    for i in range(4):
        training, test = [], []
        tl = 30
        for t in range(5):
            num_prems = 3
            correct = t % 2 == 0
            training.append(make_opposite_chain_trial(gen_id(sid, "tr", t+1), num_prems, correct, tl))
        for t in range(3):
            test.append(make_opposite_chain_trial(gen_id(sid, "te", t+1), 3, t != 1, tl))

        stages.append(make_stage(
            sid, f"Complex Opposition {i+1}",
            "Derive relations through three-step opposite chains (odd=opposite, even=same).",
            "M3_Opposition", "OPPOSITION", "OPPOSITE",
            ["SAME", "OPPOSITE"], 3, 3, "HARD",
            0.85, tl, 150 + i*10, 4, training, test
        ))
        sid += 1

    # --- Stages 48-50: ADVANCED - Mixed Same/Opposite with comparison ---
    for i in range(3):
        training, test = [], []
        tl = 30
        for t in range(5):
            if t % 3 == 0:
                training.append(make_opposite_chain_trial(gen_id(sid, "tr", t+1), 3, True, tl))
            elif t % 3 == 1:
                training.append(make_same_opposite_trial(gen_id(sid, "tr", t+1), 1, tl))
            else:
                training.append(make_opposite_chain_trial(gen_id(sid, "tr", t+1), 2, False, tl))
        for t in range(3):
            if t == 0:
                test.append(make_opposite_chain_trial(gen_id(sid, "te", t+1), 3, True, tl))
            elif t == 1:
                test.append(make_same_opposite_trial(gen_id(sid, "te", t+1), 1, tl))
            else:
                test.append(make_opposite_chain_trial(gen_id(sid, "te", t+1), 2, False, tl))

        stages.append(make_stage(
            sid, f"Advanced Opposition {i+1}",
            "Master complex opposition patterns with Same/Opposite frame combinations.",
            "M3_Opposition", "OPPOSITION", "OPPOSITE",
            ["SAME", "OPPOSITE", "DIFFERENT"], 3, 3, "ADVANCED",
            0.85, tl, 170 + i*10, 5, training, test
        ))
        sid += 1

    return stages, sid


# ============================================================
# MODULE 4: TEMPORAL (Before/After) - Stages 51-62
# ============================================================

def generate_m4_stages(start_id):
    stages = []
    sid = start_id

    # --- Stages 51-53: MEDIUM - Single/two premise ---
    for i in range(3):
        training, test = [], []
        tl = 28
        for t in range(5):
            rel = "BEFORE" if t % 2 == 0 else "AFTER"
            if t < 2:
                training.append(make_symmetry_trial(gen_id(sid, "tr", t+1), rel, True, tl))
            elif t == 2:
                training.append(make_inverse_trial(gen_id(sid, "tr", t+1), "BEFORE", 1, tl))
            else:
                training.append(make_transitive_trial(gen_id(sid, "tr", t+1), rel, 2, t == 3, tl))
        for t in range(3):
            rel = "BEFORE" if t % 2 == 0 else "AFTER"
            test.append(make_transitive_trial(gen_id(sid, "te", t+1), rel, 2, t != 1, tl))

        stages.append(make_stage(
            sid, f"Temporal Basics {i+1}",
            "Learn Before and After relations with basic temporal reasoning.",
            "M4_Temporal", "TEMPORAL", "BEFORE_AFTER",
            ["BEFORE", "AFTER"], 2, 2, "MEDIUM",
            0.80, tl, 100 + i*10, 3, training, test
        ))
        sid += 1

    # --- Stages 54-57: MEDIUM-HARD - Two premise with inverse ---
    for i in range(4):
        training, test = [], []
        tl = 25
        for t in range(5):
            rel = "BEFORE" if t % 2 == 0 else "AFTER"
            if t < 2:
                training.append(make_transitive_trial(gen_id(sid, "tr", t+1), rel, 2, True, tl))
            elif t == 2:
                training.append(make_inverse_trial(gen_id(sid, "tr", t+1), rel, 2, tl))
            else:
                training.append(make_transitive_trial(gen_id(sid, "tr", t+1), rel, 2, False, tl))
        for t in range(3):
            rel = "BEFORE" if t % 2 == 0 else "AFTER"
            test.append(make_transitive_trial(gen_id(sid, "te", t+1), rel, 2, t != 2, tl))

        stages.append(make_stage(
            sid, f"Temporal Fluency {i+1}",
            "Build speed with Before/After chains and inverse derivations.",
            "M4_Temporal", "TEMPORAL", "BEFORE_AFTER",
            ["BEFORE", "AFTER"], 2, 2, "HARD" if i >= 2 else "MEDIUM",
            0.85, tl, 120 + i*10, 4, training, test
        ))
        sid += 1

    # --- Stages 58-60: HARD - Three premise chains ---
    for i in range(3):
        training, test = [], []
        tl = 30
        for t in range(5):
            rel = "BEFORE" if t % 2 == 0 else "AFTER"
            correct = t % 2 == 0
            training.append(make_transitive_trial(gen_id(sid, "tr", t+1), rel, 3, correct, tl))
        for t in range(3):
            rel = "BEFORE"
            test.append(make_transitive_trial(gen_id(sid, "te", t+1), rel, 3, t != 1, tl))

        stages.append(make_stage(
            sid, f"Extended Temporal {i+1}",
            "Derive temporal relations across three-event sequences.",
            "M4_Temporal", "TEMPORAL", "BEFORE_AFTER",
            ["BEFORE", "AFTER"], 3, 3, "HARD",
            0.85, tl, 150 + i*10, 4, training, test
        ))
        sid += 1

    # --- Stages 61-62: ADVANCED - Four premise + mixed ---
    for i in range(2):
        training, test = [], []
        tl = 35
        for t in range(5):
            rel = "BEFORE" if t % 2 == 0 else "AFTER"
            correct = t < 3
            training.append(make_transitive_trial(gen_id(sid, "tr", t+1), rel, 4, correct, tl))
        for t in range(3):
            test.append(make_transitive_trial(gen_id(sid, "te", t+1), "BEFORE", 4, t != 1, tl))

        stages.append(make_stage(
            sid, f"Advanced Temporal {i+1}",
            "Master four-event temporal sequences for complex sequence reasoning.",
            "M4_Temporal", "TEMPORAL", "BEFORE_AFTER",
            ["BEFORE", "AFTER"], 4, 4, "ADVANCED",
            0.90, tl, 180 + i*10, 5, training, test
        ))
        sid += 1

    return stages, sid


# ============================================================
# MODULE 5: CONTAINMENT (Contains/Within) - Stages 63-72
# ============================================================

def generate_m5_stages(start_id):
    stages = []
    sid = start_id

    # --- Stages 63-65: HARD - Single/two premise containment ---
    for i in range(3):
        training, test = [], []
        tl = 30
        for t in range(5):
            if t < 2:
                training.append(make_containment_trial(gen_id(sid, "tr", t+1), 1, True, tl))
            elif t == 2:
                training.append(make_containment_trial(gen_id(sid, "tr", t+1), 1, False, tl))
            elif t == 3:
                training.append(make_containment_trial(gen_id(sid, "tr", t+1), 2, True, tl))
            else:
                training.append(make_containment_trial(gen_id(sid, "tr", t+1), 2, False, tl))
        for t in range(3):
            test.append(make_containment_trial(gen_id(sid, "te", t+1), 2, t != 1, tl))

        stages.append(make_stage(
            sid, f"Containment Basics {i+1}",
            "Learn Contains and Within relations for part-whole reasoning.",
            "M5_Containment", "CONTAINMENT", "CONTAINS_WITHIN",
            ["CONTAINS", "WITHIN"], 2, 2, "HARD",
            0.80, tl, 130 + i*10, 4, training, test
        ))
        sid += 1

    # --- Stages 66-69: HARD-ADVANCED - Two-three premise chains ---
    for i in range(4):
        training, test = [], []
        tl = 30
        n_prem = 2 if i < 2 else 3
        for t in range(5):
            if t % 3 == 0:
                training.append(make_containment_trial(gen_id(sid, "tr", t+1), n_prem, True, tl))
            elif t % 3 == 1:
                training.append(make_containment_trial(gen_id(sid, "tr", t+1), n_prem, False, tl))
            else:
                training.append(make_inverse_trial(gen_id(sid, "tr", t+1), "CONTAINS", n_prem, tl))
        for t in range(3):
            test.append(make_containment_trial(gen_id(sid, "te", t+1), n_prem, t != 1, tl))

        stages.append(make_stage(
            sid, f"Hierarchical Containment {i+1}",
            "Master hierarchical containment with multi-step chains.",
            "M5_Containment", "CONTAINMENT", "CONTAINS_WITHIN",
            ["CONTAINS", "WITHIN"], n_prem, n_prem, "ADVANCED" if i >= 2 else "HARD",
            0.85, tl, 150 + i*10, 4, training, test
        ))
        sid += 1

    # --- Stages 70-72: EXPERT - Mixed containment with Same ---
    for i in range(3):
        training, test = [], []
        tl = 35
        for t in range(5):
            if t < 2:
                training.append(make_mixed_same_rel_trial(gen_id(sid, "tr", t+1), "CONTAINS", 2, tl))
            elif t == 2:
                training.append(make_containment_trial(gen_id(sid, "tr", t+1), 3, True, tl))
            elif t == 3:
                training.append(make_containment_trial(gen_id(sid, "tr", t+1), 3, False, tl))
            else:
                training.append(make_inverse_trial(gen_id(sid, "tr", t+1), "CONTAINS", 3, tl))
        for t in range(3):
            if t == 0:
                test.append(make_mixed_same_rel_trial(gen_id(sid, "te", t+1), "CONTAINS", 2, tl))
            else:
                test.append(make_containment_trial(gen_id(sid, "te", t+1), 3, t != 1, tl))

        stages.append(make_stage(
            sid, f"Advanced Containment {i+1}",
            "Combine Same and Containment frames for complex hierarchical reasoning.",
            "M5_Containment", "CONTAINMENT", "CONTAINS_WITHIN",
            ["CONTAINS", "WITHIN", "SAME"], 3, 3, "EXPERT",
            0.85, tl, 180 + i*10, 5, training, test
        ))
        sid += 1

    return stages, sid


# ============================================================
# MODULE 6: MIXED & ADVANCED - Stages 73-90
# ============================================================

def generate_m6_stages(start_id):
    stages = []
    sid = start_id

    # --- Stages 73-77: HARD - Two premise mixed frames ---
    mixed_configs = [
        (["SAME", "MORE_THAN"], "Coordination + Comparison"),
        (["SAME", "OPPOSITE"], "Coordination + Opposition"),
        (["SAME", "BEFORE"], "Coordination + Temporal"),
        (["OPPOSITE", "MORE_THAN"], "Opposition + Comparison"),
        (["SAME", "CONTAINS"], "Coordination + Containment"),
    ]
    for i, (rels, desc) in enumerate(mixed_configs):
        training, test = [], []
        tl = 30
        for t in range(5):
            training.append(make_mixed_frame_trial(gen_id(sid, "tr", t+1), rels, tl))
        for t in range(3):
            test.append(make_mixed_frame_trial(gen_id(sid, "te", t+1), rels, tl))

        stages.append(make_stage(
            sid, f"Mixed Frames: {desc}",
            f"Combine {desc.lower()} frames in two-premise chains.",
            "M6_Mixed", "MIXED", "COMBINED",
            list(set(rels)), 2, 2, "HARD",
            0.85, tl, 150 + i*10, 4, training, test
        ))
        sid += 1

    # --- Stages 78-83: ADVANCED - Three premise mixed frames ---
    advanced_configs = [
        ["SAME", "MORE_THAN", "MORE_THAN"],
        ["SAME", "OPPOSITE", "MORE_THAN"],
        ["OPPOSITE", "SAME", "BEFORE"],
        ["MORE_THAN", "SAME", "MORE_THAN"],
        ["SAME", "CONTAINS", "CONTAINS"],
        ["BEFORE", "SAME", "BEFORE"],
    ]
    for i, rels in enumerate(advanced_configs):
        training, test = [], []
        tl = 35
        for t in range(5):
            training.append(make_mixed_frame_trial(gen_id(sid, "tr", t+1), rels, tl))
        for t in range(3):
            test.append(make_mixed_frame_trial(gen_id(sid, "te", t+1), rels, tl))

        unique_types = list(set(rels))
        stages.append(make_stage(
            sid, f"Advanced Mixed {i+1}",
            "Integrate multiple relational frames in three-premise chains.",
            "M6_Mixed", "MIXED", "COMBINED",
            unique_types, 3, 3, "ADVANCED",
            0.85, tl, 180 + i*10, 5, training, test
        ))
        sid += 1

    # --- Stages 84-87: EXPERT - Three-four premise complex mixed ---
    expert_configs = [
        ["SAME", "OPPOSITE", "MORE_THAN", "MORE_THAN"],
        ["MORE_THAN", "SAME", "MORE_THAN", "SAME"],
        ["BEFORE", "SAME", "BEFORE", "SAME"],
        ["CONTAINS", "SAME", "CONTAINS", "SAME"],
    ]
    for i, rels in enumerate(expert_configs):
        training, test = [], []
        tl = 40
        for t in range(5):
            training.append(make_mixed_frame_trial(gen_id(sid, "tr", t+1), rels, tl))
        for t in range(3):
            test.append(make_mixed_frame_trial(gen_id(sid, "te", t+1), rels, tl))

        unique_types = list(set(rels))
        stages.append(make_stage(
            sid, f"Expert Mixed {i+1}",
            "Master complex four-premise chains combining multiple relational frames.",
            "M6_Mixed", "MIXED", "COMBINED",
            unique_types, 4, 4, "EXPERT",
            0.90, tl, 200 + i*10, 6, training, test
        ))
        sid += 1

    # --- Stages 88-90: EXPERT - Analogy-style (relations between relations) ---
    for i in range(3):
        training, test = [], []
        tl = 40
        # Use complex mixed chains
        analogy_rels_options = [
            ["SAME", "OPPOSITE", "SAME"],
            ["OPPOSITE", "SAME", "OPPOSITE"],
            ["MORE_THAN", "SAME", "LESS_THAN"],
        ]
        for t in range(5):
            rels = analogy_rels_options[t % len(analogy_rels_options)]
            training.append(make_mixed_frame_trial(gen_id(sid, "tr", t+1), rels, tl))
        for t in range(3):
            rels = analogy_rels_options[t % len(analogy_rels_options)]
            test.append(make_mixed_frame_trial(gen_id(sid, "te", t+1), rels, tl))

        stages.append(make_stage(
            sid, f"Relational Analogy {i+1}",
            "Higher-order reasoning: derive relations between relations across multiple frames.",
            "M6_Mixed", "MIXED", "ANALOGY",
            ["SAME", "OPPOSITE", "MORE_THAN", "LESS_THAN", "BEFORE", "AFTER"],
            3, 3, "EXPERT",
            0.90, tl, 220 + i*10, 6, training, test
        ))
        sid += 1

    return stages, sid


# ============================================================
# Main Generation
# ============================================================

def generate_all_stages():
    all_stages = []

    m1_stages, next_id = generate_m1_stages()
    all_stages.extend(m1_stages)

    m2_stages, next_id = generate_m2_stages(next_id)
    all_stages.extend(m2_stages)

    m3_stages, next_id = generate_m3_stages(next_id)
    all_stages.extend(m3_stages)

    m4_stages, next_id = generate_m4_stages(next_id)
    all_stages.extend(m4_stages)

    m5_stages, next_id = generate_m5_stages(next_id)
    all_stages.extend(m5_stages)

    m6_stages, next_id = generate_m6_stages(next_id)
    all_stages.extend(m6_stages)

    return all_stages


def validate_stages(stages):
    """Validate the generated stages for correctness."""
    errors = []
    trial_ids = set()

    for stage in stages:
        sid = stage["id"]
        all_trials = stage.get("trainingTrials", []) + stage.get("testTrials", [])

        for trial in all_trials:
            tid = trial["id"]
            if tid in trial_ids:
                errors.append(f"Duplicate trial ID: {tid}")
            trial_ids.add(tid)

            # Check premises exist
            if not trial.get("premises"):
                errors.append(f"Trial {tid}: no premises")

            # Check probe fields
            for field in ["probeStimA", "probeRelation", "probeStimB"]:
                if not trial.get(field):
                    errors.append(f"Trial {tid}: missing {field}")

            # Validate relation types
            valid_rels = {"SAME", "DIFFERENT", "OPPOSITE", "MORE_THAN", "LESS_THAN",
                          "BEFORE", "AFTER", "CONTAINS", "WITHIN"}
            probe_rel = trial.get("probeRelation", "")
            if probe_rel not in valid_rels:
                errors.append(f"Trial {tid}: invalid probe relation '{probe_rel}'")

            for p in trial.get("premises", []):
                if p.get("relationType") not in valid_rels:
                    errors.append(f"Trial {tid}: invalid premise relation '{p.get('relationType')}'")

    return errors


if __name__ == "__main__":
    stages = generate_all_stages()

    # Validate
    errors = validate_stages(stages)
    if errors:
        print(f"VALIDATION ERRORS ({len(errors)}):")
        for e in errors:
            print(f"  - {e}")
    else:
        print(f"All {len(stages)} stages validated successfully!")

    # Print summary
    modules = {}
    for s in stages:
        m = s["module"]
        if m not in modules:
            modules[m] = []
        modules[m].append(s)

    print("\nModule Summary:")
    total_training = 0
    total_test = 0
    for m, ss in modules.items():
        t_count = sum(len(s["trainingTrials"]) for s in ss)
        te_count = sum(len(s["testTrials"]) for s in ss)
        total_training += t_count
        total_test += te_count
        print(f"  {m}: {len(ss)} stages, {t_count} training + {te_count} test trials")
    print(f"\nTotal: {len(stages)} stages, {total_training} training + {total_test} test = {total_training + total_test} trials")

    # Write output
    output_path = "app/src/main/assets/stages.json"
    with open(output_path, "w") as f:
        json.dump(stages, f, indent=2)
    print(f"\nWritten to {output_path}")
