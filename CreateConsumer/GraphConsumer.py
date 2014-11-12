import sys
import glob
import os
import numpy
import matplotlib.pyplot as plt
import subprocess
from decimal import Decimal
import itertools

import CreateConsumer

def get_max_starting_strategy(file_base, initial_belief_state):
    for file_name in glob.glob('*.alpha'):
        if file_name.startswith(file_base):
            with open(file_name, 'r') as f:
                lines = f.readlines()
                max_sum = -99999
                max_strategy = -1
                for idx, line in enumerate(lines):
                    line = line.strip()
                    if (idx % 3) == 1:
                        weights = map(Decimal, line.split())
                        belief = map(Decimal, initial_belief_state)
                        sum = numpy.dot(weights, belief)
                        if sum > max_sum:
                            max_sum = sum
                            max_strategy = (idx-1)/3
            return max_strategy

def get_list_of_actions(file_base, start):
    for file_name in glob.glob('*.pg'):
        if file_name.startswith(file_base):
            with open(file_name, 'r') as f:
                lines = f.readlines()
                lines = map(lambda(x): x.strip(), lines)
                lines = map(lambda(x): x.split(), lines)
                curr = start
                actions = []
                bargaining = True
                while bargaining:
                    actions.append(int(lines[curr][1]))
                    curr = int(lines[curr][2])
                    if curr == 0:
                        bargaining = False
                return actions

def solve_pomdp(leave_probability, num_prices, epsilon):
    discount = 0.95
    values = 'reward'
    file_base = filebase(leave_probability)

    CreateConsumer.write_pomdp(file_base + '.POMDP', discount, num_prices, values, leave_probability)
    subprocess.call(['../pomdp-solve-5.3/src/pomdp-solve', '-pomdp', file_base + '.POMDP', '-epsilon', str(epsilon)])
    return file_base


def transform_list(strategy_list):
    index_lists = {}
    # get length of longest list
    max_length = len(max(strategy_list.values(),key=len))
    for idx in range(0, max_length):
        index_list = []
        for k, v in strategy_list.items():
            if len(v) > idx:
                index_list.append((k, v[idx]))
                index_lists[idx] = sorted(index_list, key = lambda x: x[0])
    return index_lists

def parse_files(step, initial_belief_state, num_prices):
    file_strategies = {}
    for p in frange(0.00, 1.00, step):
        file_base = filebase(p)
        start = get_max_starting_strategy(file_base, initial_belief_state)
        file_strategies[p] = get_list_of_actions(file_base, start)
    return file_strategies

def frange(x, y, jump):
  r = []
  while x <= y:
    r.append(x)
    x += jump
    x = round(x, 2)
  return r

def filebase(leave_probability):
    return 'p-{0}'.format(leave_probability)

# Usage: python GraphConsumer [-solve]
# -solve means it will also create and solve the pomdp in addition to graphing
def main():
    # Handle arguments
    solve = False
    if (len(sys.argv) == 2) and (sys.argv[1] == '-solve'):
        solve = True

    # Set parameters
    num_prices = 7
    step = 0.05
    belief_dist = 'uniform'
    epsilon = 0.000001

    # Create the initial belief state based on
    if belief_dist == 'uniform':
        belief_state = [1.0/num_prices] * num_prices
        belief_state.append(0)
    else:
        belief_state = [1.0/num_prices] * num_prices
        belief_state.append(0)

    # Create and Solve the POMDP if requested
    if solve:
        for p in frange(0.00, 1.00, step):
            solve_pomdp(p, num_prices, epsilon)

    # Parse, print, and graph the strategies
    file_strategies = parse_files(step, belief_state, num_prices)
    print file_strategies
    graph_strategies = transform_list(file_strategies)
    print graph_strategies
    marker = itertools.cycle((',', '+', '.', 'o', '*'))
    for i in range(0, len(graph_strategies)):
        data = zip(*(graph_strategies[i]))
        plt.scatter(*data, marker = marker.next())
        plt.plot(*data)
    plt.show()


if __name__ == '__main__':
    main()
