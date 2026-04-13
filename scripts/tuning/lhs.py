#!/usr/bin/env python3
"""Latin Hypercube Sampling for transport tuning parameter generation.

Generates N parameter sets spread across the search space, plus
a baseline config (config_000) using default values.

Usage:
    python lhs.py <search_space_yaml> <output_dir> [--n 9] [--seed 42]

    # Example: generate 10 configs (1 baseline + 9 LHS) for pref 22
    python lhs.py ../../config/tuning/transport_search_space.yaml \
        ../../output/tuning/22/configs --n 9 --seed 42 \
        --output-base /path/to/output/tuning/22/stage1
"""

import argparse
import os
import random
import sys
from pathlib import Path

try:
    import yaml
except ImportError:
    # Inline minimal YAML parser for the simple search space format
    yaml = None


def parse_search_space_simple(path):
    """Parse search space YAML without PyYAML dependency.

    Handles only the flat structure used by transport_search_space.yaml.
    """
    params = {}
    current_param = None
    with open(path) as f:
        in_parameters = False
        for line in f:
            stripped = line.strip()
            if stripped.startswith("#") or not stripped:
                continue
            if stripped == "parameters:":
                in_parameters = True
                continue
            if in_parameters and not line.startswith("  "):
                in_parameters = False
                continue
            if in_parameters:
                indent = len(line) - len(line.lstrip())
                if indent == 2 and stripped.endswith(":"):
                    current_param = stripped[:-1]
                    params[current_param] = {}
                elif indent == 4 and current_param:
                    key, _, val = stripped.partition(":")
                    val = val.strip().strip('"').strip("'")
                    if key == "range":
                        # Parse [min, max]
                        val = val.strip("[]")
                        parts = [x.strip() for x in val.split(",")]
                        params[current_param]["range"] = [float(parts[0]), float(parts[1])]
                    elif key == "default":
                        params[current_param]["default"] = float(val)
                    elif key == "round":
                        params[current_param]["round"] = val.lower() == "true"
                    elif key == "description":
                        params[current_param]["description"] = val
    return params


def load_search_space(path):
    """Load search space from YAML file."""
    if yaml:
        with open(path) as f:
            data = yaml.safe_load(f)
        return data["parameters"]
    else:
        return parse_search_space_simple(path)


def latin_hypercube_sample(params, n, seed=42):
    """Generate n parameter sets using Latin Hypercube Sampling.

    Each parameter's range is divided into n equal strata.
    One sample is drawn from each stratum, then columns are shuffled.
    """
    rng = random.Random(seed)
    param_names = sorted(params.keys())
    k = len(param_names)

    # For each parameter, generate one sample per stratum
    samples = {}
    for name in param_names:
        lo, hi = params[name]["range"]
        stratum_width = (hi - lo) / n
        values = []
        for i in range(n):
            # Uniform sample within stratum i
            v = lo + stratum_width * (i + rng.random())
            if params[name].get("round"):
                v = round(v)
            else:
                v = round(v, 4)
            values.append(v)
        rng.shuffle(values)
        samples[name] = values

    # Build list of config dicts
    configs = []
    for i in range(n):
        cfg = {}
        for name in param_names:
            cfg[name] = samples[name][i]
        configs.append(cfg)

    return configs


def write_config(filepath, params, output_dir=None):
    """Write a candidate config .properties file.

    Args:
        filepath: Output path for the .properties file.
        params: Dict of parameter_key -> value.
        output_dir: If set, adds outputDir= line.
    """
    os.makedirs(os.path.dirname(filepath), exist_ok=True)
    with open(filepath, "w") as f:
        f.write(f"# Generated tuning config: {os.path.basename(filepath)}\n")
        if output_dir:
            # Java Properties treats '\' as an escape character, so raw
            # Windows paths get mangled on load. Always serialize paths
            # with forward slashes — Java accepts them on all platforms.
            output_dir_fs = str(output_dir).replace("\\", "/")
            f.write(f"outputDir={output_dir_fs}\n")
        for key in sorted(params.keys()):
            val = params[key]
            # Write integers without decimal point
            if isinstance(val, float) and val == int(val):
                f.write(f"{key}={int(val)}\n")
            else:
                f.write(f"{key}={val}\n")


def generate_configs(search_space_path, output_dir, n=9, seed=42, output_base=None):
    """Generate baseline + n LHS configs.

    Args:
        search_space_path: Path to transport_search_space.yaml.
        output_dir: Directory to write config_NNN.properties files.
        n: Number of LHS samples (total configs = n + 1 including baseline).
        seed: Random seed for reproducibility.
        output_base: Base path for candidate outputDir (e.g., output/tuning/22/stage1).

    Returns:
        List of (config_id, filepath, params_dict).
    """
    params = load_search_space(search_space_path)
    os.makedirs(output_dir, exist_ok=True)

    configs = []

    # Config 000: baseline (all defaults)
    baseline = {name: info["default"] for name, info in params.items()}
    cfg_path = os.path.join(output_dir, "config_000.properties")
    candidate_output = os.path.join(output_base, "000") + "/" if output_base else None
    write_config(cfg_path, baseline, candidate_output)
    configs.append(("000", cfg_path, baseline))

    # Configs 001-N: LHS samples
    lhs_configs = latin_hypercube_sample(params, n, seed=seed)
    for i, cfg in enumerate(lhs_configs):
        cfg_id = f"{i + 1:03d}"
        cfg_path = os.path.join(output_dir, f"config_{cfg_id}.properties")
        candidate_output = os.path.join(output_base, cfg_id) + "/" if output_base else None
        write_config(cfg_path, cfg, candidate_output)
        configs.append((cfg_id, cfg_path, cfg))

    return configs


def print_configs(configs, params):
    """Print a summary table of generated configs."""
    param_names = sorted(params.keys())
    # Header
    header = f"{'ID':>4}"
    for name in param_names:
        short = name.split(".")[-1][:12]
        header += f"  {short:>12}"
    print(header)
    print("-" * len(header))

    for cfg_id, _, values in configs:
        line = f"{cfg_id:>4}"
        for name in param_names:
            v = values[name]
            if isinstance(v, float) and v == int(v):
                line += f"  {int(v):>12}"
            else:
                line += f"  {v:>12}"
        print(line)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Generate LHS parameter configs for transport tuning")
    parser.add_argument("search_space", help="Path to transport_search_space.yaml")
    parser.add_argument("output_dir", help="Directory to write config_NNN.properties")
    parser.add_argument("--n", type=int, default=9, help="Number of LHS samples (default: 9)")
    parser.add_argument("--seed", type=int, default=42, help="Random seed (default: 42)")
    parser.add_argument("--output-base", help="Base path for candidate outputDir")
    args = parser.parse_args()

    params = load_search_space(args.search_space)
    configs = generate_configs(
        args.search_space, args.output_dir,
        n=args.n, seed=args.seed, output_base=args.output_base,
    )

    print(f"Generated {len(configs)} configs in {args.output_dir}/\n")
    print_configs(configs, params)
