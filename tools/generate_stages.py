#!/usr/bin/env python3
"""
Stage content generator for RelationalIQ.
Generates validated JSON stage files for the Android app's relational training system.
"""

import json
import os
import random
import string
import argparse
from pathlib import Path

STAGES_DIR = Path(__file__).parent.parent / "app" / "src" / "main" / "assets" / "stages"


def generate_nonsense_word(length=3, exclude=None):
    """Generate a random nonsense word of given length."""
    if exclude is None:
        exclude = set()
    consonants = "BCDFGHJKLMNPQRSTVWXYZ"
    vowels = "AEIOU"
    while True:
        word = ""
        for i in range(length):
            if i % 2 == 0:
                word += random.choice(consonants)
            else:
                word += random.choice(vowels)
        if word not in exclude:
            return word


def generate_stimuli(count, length=3, existing=None):
    """Generate a set of unique nonsense words."""
    if existing is None:
        existing = set()
    stimuli = []
    used = set(existing)
    for _ in range(count):
        word = generate_nonsense_word(length, used)
        used.add(word)
        stimuli.append(word)
    return stimuli


def make_premise(stim_a, relation, stim_b):
    return {"stimulusA": stim_a, "relationType": relation, "stimulusB": stim_b}


def make_trial(trial_id, premises, probe_a, probe_rel, probe_b, correct, explanation="", time_limit=25):
    trial = {
        "id": trial_id,
        "premises": premises,
        "probeStimA": probe_a,
        "probeRelation": probe_rel,
        "probeStimB": probe_b,
        "correctAnswer": correct,
        "timeLimitSeconds": time_limit
    }
    if explanation:
        trial["explanation"] = explanation
    return trial


def generate_comparison_trial(trial_id, stim_length=3, num_premises=2, time_limit=25):
    """Generate a basic more/less transitive trial."""
    stimuli = generate_stimuli(num_premises + 1, stim_length)
    # Create a chain: stimuli[0] > stimuli[1] > ... > stimuli[n]
    premises = []
    use_less = random.choice([True, False])
    
    if use_less:
        # Build from bottom: stimuli[n] < stimuli[n-1] < ... < stimuli[0]
        for i in range(num_premises - 1, -1, -1):
            premises.append(make_premise(stimuli[i+1], "LESS_THAN", stimuli[i]))
        # Ask about endpoints
        correct = random.choice([True, False])
        if correct:
            probe_a = stimuli[-1]
            probe_rel = "LESS_THAN"
            probe_b = stimuli[0]
            explanation = f"{' < '.join(reversed(stimuli))}, so {stimuli[-1]} is less than {stimuli[0]}."
        else:
            probe_a = stimuli[0]
            probe_rel = "LESS_THAN"
            probe_b = stimuli[-1]
            explanation = f"{' > '.join(stimuli)}, so {stimuli[0]} is NOT less than {stimuli[-1]}."
    else:
        for i in range(num_premises):
            premises.append(make_premise(stimuli[i], "MORE_THAN", stimuli[i+1]))
        correct = random.choice([True, False])
        if correct:
            probe_a = stimuli[0]
            probe_rel = "MORE_THAN"
            probe_b = stimuli[-1]
            explanation = f"{' > '.join(stimuli)}, so {stimuli[0]} is more than {stimuli[-1]}."
        else:
            probe_a = stimuli[-1]
            probe_rel = "MORE_THAN"
            probe_b = stimuli[0]
            explanation = f"{' > '.join(stimuli)}, so {stimuli[-1]} is NOT more than {stimuli[0]}."
    
    return make_trial(trial_id, premises, probe_a, probe_rel, probe_b, correct, explanation, time_limit)


def generate_same_comparison_trial(trial_id, stim_length=4, time_limit=30):
    """Generate a Same + Comparison trial (equivalence + derivation)."""
    stimuli = generate_stimuli(3, stim_length)
    # A is same as B, B is more/less than C
    use_more = random.choice([True, False])
    
    premises = [
        make_premise(stimuli[0], "SAME", stimuli[1])
    ]
    
    if use_more:
        premises.append(make_premise(stimuli[1], "MORE_THAN", stimuli[2]))
        correct = random.choice([True, False])
        if correct:
            probe_a = stimuli[0]
            probe_rel = "MORE_THAN"
            probe_b = stimuli[2]
            explanation = f"{stimuli[0]} = {stimuli[1]} and {stimuli[1]} > {stimuli[2]}, so {stimuli[0]} > {stimuli[2]}."
        else:
            probe_a = stimuli[2]
            probe_rel = "MORE_THAN"
            probe_b = stimuli[0]
            explanation = f"{stimuli[0]} = {stimuli[1]} > {stimuli[2]}, so {stimuli[2]} is NOT more than {stimuli[0]}."
    else:
        premises.append(make_premise(stimuli[1], "LESS_THAN", stimuli[2]))
        correct = random.choice([True, False])
        if correct:
            probe_a = stimuli[0]
            probe_rel = "LESS_THAN"
            probe_b = stimuli[2]
            explanation = f"{stimuli[0]} = {stimuli[1]} and {stimuli[1]} < {stimuli[2]}, so {stimuli[0]} < {stimuli[2]}."
        else:
            probe_a = stimuli[0]
            probe_rel = "MORE_THAN"
            probe_b = stimuli[2]
            explanation = f"{stimuli[0]} = {stimuli[1]} < {stimuli[2]}, so {stimuli[0]} is NOT more than {stimuli[2]}."
    
    return make_trial(trial_id, premises, probe_a, probe_rel, probe_b, correct, explanation, time_limit)


def generate_opposition_trial(trial_id, stim_length=4, time_limit=30):
    """Generate an Opposition + Comparison trial."""
    stimuli = generate_stimuli(3, stim_length)
    variant = random.choice(["opp_same", "opp_comparison", "double_opp"])
    
    if variant == "opp_same":
        # A same as B, B opposite C -> A opposite C
        premises = [
            make_premise(stimuli[0], "SAME", stimuli[1]),
            make_premise(stimuli[1], "OPPOSITE", stimuli[2])
        ]
        correct = random.choice([True, False])
        if correct:
            probe_a = stimuli[0]
            probe_rel = "OPPOSITE"
            probe_b = stimuli[2]
            explanation = f"{stimuli[0]} = {stimuli[1]} and {stimuli[1]} is opposite {stimuli[2]}, so {stimuli[0]} is opposite {stimuli[2]}."
        else:
            probe_a = stimuli[0]
            probe_rel = "SAME"
            probe_b = stimuli[2]
            explanation = f"{stimuli[0]} = {stimuli[1]} and {stimuli[1]} is opposite {stimuli[2]}, so {stimuli[0]} is NOT same as {stimuli[2]}."
    elif variant == "opp_comparison":
        # A opposite B, B more than C -> A less than C
        premises = [
            make_premise(stimuli[0], "OPPOSITE", stimuli[1]),
            make_premise(stimuli[1], "MORE_THAN", stimuli[2])
        ]
        correct = random.choice([True, False])
        if correct:
            probe_a = stimuli[0]
            probe_rel = "LESS_THAN"
            probe_b = stimuli[2]
            explanation = f"{stimuli[0]} is opposite {stimuli[1]}, and {stimuli[1]} > {stimuli[2]}. Opposite reverses, so {stimuli[0]} < {stimuli[2]}."
        else:
            probe_a = stimuli[0]
            probe_rel = "MORE_THAN"
            probe_b = stimuli[2]
            explanation = f"{stimuli[0]} is opposite {stimuli[1]}, and {stimuli[1]} > {stimuli[2]}. Opposite reverses, so {stimuli[0]} is NOT more than {stimuli[2]}."
    else:
        # A opposite B, B opposite C -> A same as C
        premises = [
            make_premise(stimuli[0], "OPPOSITE", stimuli[1]),
            make_premise(stimuli[1], "OPPOSITE", stimuli[2])
        ]
        correct = random.choice([True, False])
        if correct:
            probe_a = stimuli[0]
            probe_rel = "SAME"
            probe_b = stimuli[2]
            explanation = f"{stimuli[0]} is opposite {stimuli[1]}, {stimuli[1]} is opposite {stimuli[2]}. Opposite of opposite = same."
        else:
            probe_a = stimuli[0]
            probe_rel = "OPPOSITE"
            probe_b = stimuli[2]
            explanation = f"{stimuli[0]} is opposite {stimuli[1]}, {stimuli[1]} is opposite {stimuli[2]}. Opposite of opposite = same, not opposite."
    
    return make_trial(trial_id, premises, probe_a, probe_rel, probe_b, correct, explanation, time_limit)


def generate_mixed_trial(trial_id, stim_length=4, num_premises=3, time_limit=35):
    """Generate a complex mixed-relation trial."""
    stimuli = generate_stimuli(num_premises + 1, stim_length)
    variant = random.choice(["same_chain", "opp_chain", "mixed_chain"])
    
    if variant == "same_chain":
        # A = B, B > C, C > D
        premises = [
            make_premise(stimuli[0], "SAME", stimuli[1]),
            make_premise(stimuli[1], "MORE_THAN", stimuli[2])
        ]
        if num_premises >= 3:
            premises.append(make_premise(stimuli[2], "MORE_THAN", stimuli[3]))
        correct = True
        probe_a = stimuli[0]
        probe_rel = "MORE_THAN"
        probe_b = stimuli[-1]
        chain = f"{stimuli[0]} = {stimuli[1]} > {stimuli[2]}"
        if num_premises >= 3:
            chain += f" > {stimuli[3]}"
        explanation = f"{chain}, so {stimuli[0]} > {stimuli[-1]}."
    elif variant == "opp_chain":
        # A opposite B, B > C -> A < C
        premises = [
            make_premise(stimuli[0], "OPPOSITE", stimuli[1]),
            make_premise(stimuli[1], "MORE_THAN", stimuli[2])
        ]
        if num_premises >= 3:
            premises.append(make_premise(stimuli[2], "MORE_THAN", stimuli[3]))
        correct = True
        probe_a = stimuli[0]
        probe_rel = "LESS_THAN"
        probe_b = stimuli[-1]
        explanation = f"{stimuli[0]} is opposite {stimuli[1]}, and {stimuli[1]} > {stimuli[2]}. Opposition reverses the relation."
    else:
        # A = B, B = C, C > D
        premises = [
            make_premise(stimuli[0], "SAME", stimuli[1]),
            make_premise(stimuli[1], "SAME", stimuli[2])
        ]
        if num_premises >= 3:
            premises.append(make_premise(stimuli[2], "MORE_THAN", stimuli[3]))
            correct = True
            probe_a = stimuli[0]
            probe_rel = "MORE_THAN"
            probe_b = stimuli[3]
            explanation = f"{stimuli[0]} = {stimuli[1]} = {stimuli[2]} > {stimuli[3]}, so {stimuli[0]} > {stimuli[3]}."
        else:
            correct = True
            probe_a = stimuli[0]
            probe_rel = "SAME"
            probe_b = stimuli[2]
            explanation = f"{stimuli[0]} = {stimuli[1]} = {stimuli[2]}, so {stimuli[0]} is same as {stimuli[2]}."
    
    return make_trial(trial_id, premises, probe_a, probe_rel, probe_b, correct, explanation, time_limit)


def generate_stage(stage_id, stage_config):
    """Generate a complete stage with training and test trials."""
    stage_type = stage_config["type"]
    stim_length = stage_config.get("stim_length", 3)
    num_training = stage_config.get("training_count", 5)
    num_test = stage_config.get("test_count", 3)
    num_premises = stage_config.get("premises", 2)
    time_limit = stage_config.get("time_limit", 25)
    
    random.seed(stage_id * 42)  # Reproducible
    
    training_trials = []
    test_trials = []
    
    generator = {
        "comparison": lambda tid: generate_comparison_trial(tid, stim_length, num_premises, time_limit),
        "same_comparison": lambda tid: generate_same_comparison_trial(tid, stim_length, time_limit),
        "opposition": lambda tid: generate_opposition_trial(tid, stim_length, time_limit),
        "mixed": lambda tid: generate_mixed_trial(tid, stim_length, num_premises, time_limit),
    }[stage_type]
    
    for i in range(num_training):
        trial = generator(f"s{stage_id:03d}_t{i+1:02d}")
        training_trials.append(trial)
    
    for i in range(num_test):
        trial = generator(f"s{stage_id:03d}_x{i+1:02d}")
        # Test trials don't have explanations
        if "explanation" in trial:
            del trial["explanation"]
        test_trials.append(trial)
    
    # Ensure at least one True and one False answer in training
    has_true = any(t["correctAnswer"] for t in training_trials)
    has_false = any(not t["correctAnswer"] for t in training_trials)
    if not has_true and training_trials:
        training_trials[0]["correctAnswer"] = True
    if not has_false and len(training_trials) > 1:
        training_trials[-1]["correctAnswer"] = False
    
    return {
        "id": stage_id,
        "title": stage_config["title"],
        "description": stage_config["description"],
        "relationTypes": stage_config["relation_types"],
        "premiseCount": num_premises,
        "difficulty": stage_config["difficulty"],
        "masteryThreshold": stage_config.get("mastery", 0.85),
        "timeLimitSeconds": time_limit,
        "xpReward": stage_config.get("xp", 100),
        "trainingTrials": training_trials,
        "testTrials": test_trials
    }


# Stage configurations for stages 5-40
STAGE_CONFIGS = {
    5: {"type": "comparison", "title": "Three-Step Chains", "description": "Derive relations across three-item transitive chains with more than.", "relation_types": ["MORE_THAN", "LESS_THAN"], "stim_length": 3, "premises": 2, "training_count": 5, "test_count": 3, "time_limit": 25, "difficulty": "EASY", "xp": 120},
    6: {"type": "comparison", "title": "Longer Chains I", "description": "Extended transitive chains requiring multi-step reasoning.", "relation_types": ["MORE_THAN", "LESS_THAN"], "stim_length": 3, "premises": 2, "training_count": 5, "test_count": 3, "time_limit": 25, "difficulty": "EASY", "xp": 130},
    7: {"type": "comparison", "title": "Longer Chains II", "description": "Continue building transitive reasoning fluency.", "relation_types": ["MORE_THAN", "LESS_THAN"], "stim_length": 3, "premises": 2, "training_count": 5, "test_count": 3, "time_limit": 25, "difficulty": "EASY", "xp": 130},
    8: {"type": "comparison", "title": "Varied Direction Queries", "description": "Questions ask about relations in unexpected directions.", "relation_types": ["MORE_THAN", "LESS_THAN"], "stim_length": 3, "premises": 2, "training_count": 5, "test_count": 3, "time_limit": 25, "difficulty": "MEDIUM", "xp": 140},
    9: {"type": "comparison", "title": "Redundant Premise Training", "description": "Learn to handle extra information that confirms existing relations.", "relation_types": ["MORE_THAN", "LESS_THAN"], "stim_length": 3, "premises": 3, "training_count": 5, "test_count": 3, "time_limit": 30, "difficulty": "MEDIUM", "xp": 150},
    10: {"type": "comparison", "title": "Comparison Mastery", "description": "Master basic comparison with longer chains and mixed directions.", "relation_types": ["MORE_THAN", "LESS_THAN"], "stim_length": 3, "premises": 3, "training_count": 6, "test_count": 3, "time_limit": 30, "difficulty": "MEDIUM", "xp": 160},
    11: {"type": "same_comparison", "title": "Same + Comparison Intro", "description": "Learn to derive relations across 'is the same as' combined with comparison.", "relation_types": ["SAME", "MORE_THAN", "LESS_THAN"], "stim_length": 4, "premises": 2, "training_count": 5, "test_count": 3, "time_limit": 30, "difficulty": "EASY", "xp": 140},
    12: {"type": "same_comparison", "title": "Equivalence Derivation", "description": "Derive comparisons through equivalence chains.", "relation_types": ["SAME", "MORE_THAN", "LESS_THAN"], "stim_length": 4, "premises": 2, "training_count": 5, "test_count": 3, "time_limit": 30, "difficulty": "EASY", "xp": 150},
    13: {"type": "same_comparison", "title": "Same + Less Than", "description": "Practice deriving less-than relations through equivalence.", "relation_types": ["SAME", "MORE_THAN", "LESS_THAN"], "stim_length": 4, "premises": 2, "training_count": 5, "test_count": 3, "time_limit": 30, "difficulty": "EASY", "xp": 150},
    14: {"type": "same_comparison", "title": "Bidirectional Sameness", "description": "Understand that sameness works in both directions.", "relation_types": ["SAME", "MORE_THAN", "LESS_THAN"], "stim_length": 4, "premises": 2, "training_count": 5, "test_count": 3, "time_limit": 30, "difficulty": "MEDIUM", "xp": 160},
    15: {"type": "same_comparison", "title": "Three Premise Coordination", "description": "Coordinate three premises involving sameness and comparison.", "relation_types": ["SAME", "MORE_THAN", "LESS_THAN"], "stim_length": 4, "premises": 3, "training_count": 5, "test_count": 3, "time_limit": 35, "difficulty": "MEDIUM", "xp": 170},
    16: {"type": "same_comparison", "title": "Same Chain + Comparison", "description": "Build longer derivation chains combining sameness and magnitude.", "relation_types": ["SAME", "MORE_THAN", "LESS_THAN"], "stim_length": 4, "premises": 3, "training_count": 5, "test_count": 3, "time_limit": 35, "difficulty": "MEDIUM", "xp": 170},
    17: {"type": "same_comparison", "title": "Redundant Same Relations", "description": "Handle redundant sameness information in premises.", "relation_types": ["SAME", "MORE_THAN", "LESS_THAN"], "stim_length": 4, "premises": 3, "training_count": 5, "test_count": 3, "time_limit": 35, "difficulty": "MEDIUM", "xp": 180},
    18: {"type": "same_comparison", "title": "Mixed Same/Comparison I", "description": "Complex trials mixing coordination and comparison frames.", "relation_types": ["SAME", "MORE_THAN", "LESS_THAN"], "stim_length": 4, "premises": 3, "training_count": 5, "test_count": 3, "time_limit": 35, "difficulty": "MEDIUM", "xp": 180},
    19: {"type": "same_comparison", "title": "Mixed Same/Comparison II", "description": "Further practice with mixed coordination and comparison.", "relation_types": ["SAME", "MORE_THAN", "LESS_THAN"], "stim_length": 4, "premises": 3, "training_count": 5, "test_count": 3, "time_limit": 35, "difficulty": "MEDIUM", "xp": 190},
    20: {"type": "mixed", "title": "Complex Coordination", "description": "Advanced coordination with multi-step derivation chains.", "relation_types": ["SAME", "MORE_THAN", "LESS_THAN"], "stim_length": 4, "premises": 3, "training_count": 5, "test_count": 3, "time_limit": 35, "difficulty": "HARD", "xp": 200},
    21: {"type": "mixed", "title": "Derived Relations I", "description": "Derive complex relations through multi-premise chains.", "relation_types": ["SAME", "MORE_THAN", "LESS_THAN"], "stim_length": 4, "premises": 3, "training_count": 5, "test_count": 3, "time_limit": 35, "difficulty": "HARD", "xp": 200},
    22: {"type": "mixed", "title": "Derived Relations II", "description": "Continue building derived relational responding fluency.", "relation_types": ["SAME", "MORE_THAN", "LESS_THAN"], "stim_length": 4, "premises": 3, "training_count": 5, "test_count": 3, "time_limit": 35, "difficulty": "HARD", "xp": 210},
    23: {"type": "mixed", "title": "Multi-Step Derivation", "description": "Require 2-3 derivation steps to reach the correct answer.", "relation_types": ["SAME", "MORE_THAN", "LESS_THAN"], "stim_length": 4, "premises": 3, "training_count": 6, "test_count": 3, "time_limit": 40, "difficulty": "HARD", "xp": 220},
    24: {"type": "mixed", "title": "Coordination Fluency", "description": "Build speed and accuracy with complex coordination trials.", "relation_types": ["SAME", "MORE_THAN", "LESS_THAN"], "stim_length": 4, "premises": 3, "training_count": 6, "test_count": 3, "time_limit": 40, "difficulty": "HARD", "xp": 220},
    25: {"type": "comparison", "title": "Deeper Chain Derivation", "description": "Derive relations across 4-item transitive comparison chains.", "relation_types": ["MORE_THAN", "LESS_THAN"], "stim_length": 4, "premises": 3, "training_count": 5, "test_count": 3, "time_limit": 40, "difficulty": "HARD", "xp": 230},
    26: {"type": "opposition", "title": "Opposition Intro", "description": "Learn basic opposite relations and how they interact with sameness.", "relation_types": ["SAME", "OPPOSITE"], "stim_length": 4, "premises": 2, "training_count": 5, "test_count": 3, "time_limit": 30, "difficulty": "MEDIUM", "xp": 170},
    27: {"type": "opposition", "title": "Opposite + Same", "description": "Derive opposite relations through sameness chains.", "relation_types": ["SAME", "OPPOSITE"], "stim_length": 4, "premises": 2, "training_count": 5, "test_count": 3, "time_limit": 30, "difficulty": "MEDIUM", "xp": 180},
    28: {"type": "opposition", "title": "Opposite + Comparison", "description": "Opposition reverses comparison relations.", "relation_types": ["SAME", "OPPOSITE", "MORE_THAN", "LESS_THAN"], "stim_length": 4, "premises": 2, "training_count": 5, "test_count": 3, "time_limit": 30, "difficulty": "MEDIUM", "xp": 180},
    29: {"type": "opposition", "title": "Double Opposition", "description": "Opposite of opposite equals same.", "relation_types": ["SAME", "OPPOSITE"], "stim_length": 4, "premises": 2, "training_count": 5, "test_count": 3, "time_limit": 30, "difficulty": "HARD", "xp": 200},
    30: {"type": "opposition", "title": "Opposition Reversal", "description": "Master how opposition reverses comparison direction.", "relation_types": ["SAME", "OPPOSITE", "MORE_THAN", "LESS_THAN"], "stim_length": 4, "premises": 2, "training_count": 5, "test_count": 3, "time_limit": 35, "difficulty": "HARD", "xp": 200},
    31: {"type": "opposition", "title": "Mixed Opposition I", "description": "Combine opposition with multiple relation types.", "relation_types": ["SAME", "OPPOSITE", "MORE_THAN", "LESS_THAN"], "stim_length": 4, "premises": 3, "training_count": 5, "test_count": 3, "time_limit": 35, "difficulty": "HARD", "xp": 210},
    32: {"type": "opposition", "title": "Mixed Opposition II", "description": "Advanced opposition training with three premises.", "relation_types": ["SAME", "OPPOSITE", "MORE_THAN", "LESS_THAN"], "stim_length": 4, "premises": 3, "training_count": 5, "test_count": 3, "time_limit": 35, "difficulty": "HARD", "xp": 210},
    33: {"type": "mixed", "title": "Triple Mixed Relations", "description": "Three premises combining same, opposite, and comparison.", "relation_types": ["SAME", "OPPOSITE", "MORE_THAN", "LESS_THAN"], "stim_length": 4, "premises": 3, "training_count": 5, "test_count": 3, "time_limit": 35, "difficulty": "HARD", "xp": 220},
    34: {"type": "mixed", "title": "Complex Mixed I", "description": "Complex multi-step derivations with all relation types.", "relation_types": ["SAME", "OPPOSITE", "MORE_THAN", "LESS_THAN"], "stim_length": 4, "premises": 3, "training_count": 6, "test_count": 3, "time_limit": 40, "difficulty": "ADVANCED", "xp": 240},
    35: {"type": "mixed", "title": "Complex Mixed II", "description": "Further complex mixed-relation derivation training.", "relation_types": ["SAME", "OPPOSITE", "MORE_THAN", "LESS_THAN"], "stim_length": 4, "premises": 3, "training_count": 6, "test_count": 3, "time_limit": 40, "difficulty": "ADVANCED", "xp": 240},
    36: {"type": "mixed", "title": "Four Premise Chains", "description": "Extended chains with 4 premises requiring deep derivation.", "relation_types": ["SAME", "OPPOSITE", "MORE_THAN", "LESS_THAN"], "stim_length": 4, "premises": 4, "training_count": 5, "test_count": 3, "time_limit": 45, "difficulty": "ADVANCED", "xp": 260},
    37: {"type": "mixed", "title": "All Relations Combined I", "description": "Master-level training combining all relation types in complex chains.", "relation_types": ["SAME", "OPPOSITE", "MORE_THAN", "LESS_THAN"], "stim_length": 4, "premises": 4, "training_count": 5, "test_count": 3, "time_limit": 45, "difficulty": "ADVANCED", "xp": 260},
    38: {"type": "mixed", "title": "All Relations Combined II", "description": "Continue mastering complex multi-relation derivation.", "relation_types": ["SAME", "OPPOSITE", "MORE_THAN", "LESS_THAN"], "stim_length": 4, "premises": 4, "training_count": 6, "test_count": 3, "time_limit": 45, "difficulty": "ADVANCED", "xp": 280},
    39: {"type": "mixed", "title": "Expert Derivation I", "description": "Expert-level relational derivation with 4-5 premises and deep chains.", "relation_types": ["SAME", "OPPOSITE", "MORE_THAN", "LESS_THAN"], "stim_length": 5, "premises": 4, "training_count": 6, "test_count": 3, "time_limit": 50, "difficulty": "EXPERT", "xp": 300},
    40: {"type": "mixed", "title": "Expert Derivation II", "description": "The ultimate relational training challenge with maximum complexity.", "relation_types": ["SAME", "OPPOSITE", "MORE_THAN", "LESS_THAN"], "stim_length": 5, "premises": 4, "training_count": 6, "test_count": 4, "time_limit": 50, "difficulty": "EXPERT", "xp": 320},
}


def main():
    parser = argparse.ArgumentParser(description="Generate RelationalIQ training stages")
    parser.add_argument("--start", type=int, default=5, help="Start stage number")
    parser.add_argument("--end", type=int, default=40, help="End stage number")
    parser.add_argument("--output", type=str, default=str(STAGES_DIR), help="Output directory")
    args = parser.parse_args()
    
    output_dir = Path(args.output)
    output_dir.mkdir(parents=True, exist_ok=True)
    
    for stage_id in range(args.start, args.end + 1):
        if stage_id not in STAGE_CONFIGS:
            print(f"Skipping stage {stage_id} (no config)")
            continue
        
        config = STAGE_CONFIGS[stage_id]
        stage = generate_stage(stage_id, config)
        
        filename = f"stage_{stage_id:03d}.json"
        filepath = output_dir / filename
        
        with open(filepath, "w") as f:
            json.dump(stage, f, indent=2)
        
        print(f"Generated {filename}: {len(stage['trainingTrials'])} training + {len(stage['testTrials'])} test trials")
    
    print(f"\nDone! Generated stages {args.start}-{args.end} in {output_dir}")


if __name__ == "__main__":
    main()
