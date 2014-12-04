import sys
import glob
import os
import numpy
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import subprocess
from decimal import Decimal
import itertools
import pprint

import CreateDouble

def get_max_starting_strategy(dirname, file_base, initial_belief_state):
    for file_name in glob.glob(os.path.join(dirname, '*.alpha')):
        if file_name.startswith(os.path.join(dirname, file_base + '-')):
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

def get_pg_files_in_order(dirname, file_base):
    files = []
    for file_name in glob.glob(os.path.join(dirname, '*.pg*')):
        if file_name.startswith(os.path.join(dirname, file_base + '-')):
            if file_name[-1] == 'g':
                continue
            files.append(file_name)
    return sorted(files, key = lambda x: int(x.split(".pg")[1]), reverse=True)


def get_list_of_actions_for_multiple_pgs(dirname, file_base, start):
    files = get_pg_files_in_order(dirname, file_base)
    curr = start
    actions = []
    bargaining = True
    for file_name in files:
        if bargaining == False:
            break
        with open(file_name, 'r') as f:
            lines = f.readlines()
            lines = map(lambda(x): x.strip(), lines)
            lines = map(lambda(x): x.split(), lines)
            actions.append(int(lines[curr][1]))
            curr = int(lines[curr][2])
            if curr == 0:
                bargaining = False
    return actions

def get_list_of_actions_for_single_pg(dirname, file_base, start):
    for file_name in glob.glob(os.path.join(dirname, '*.pg')):
        if file_name.startswith(os.path.join(dirname, file_base + '-')):
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

def solve_pomdp(dirname, num_prices, num_leave_probs, epsilon, horizon, save_all):
    discount = 0.95
    values = 'reward'
    file_base = filebase(num_prices, num_leave_probs)
    file_name = os.path.join(dirname, file_base + '.POMDP')

    CreateDouble.write_pomdp(file_name, discount, num_prices, values, num_leave_probs)
    if save_all:
        subprocess.call(['../pomdp-solve-5.3/src/pomdp-solve', '-pomdp', file_name, '-epsilon', str(epsilon), '-horizon', str(horizon), '-save_all'])
    else:
        subprocess.call(['../pomdp-solve-5.3/src/pomdp-solve', '-pomdp', file_name, '-epsilon', str(epsilon), '-horizon', str(horizon)])

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

def straighten_list(strategy_list):
    max_length = len(max(strategy_list.values(),key=len))
    for strategy in strategy_list.values():
        l = len(strategy)
        strategy.extend([strategy[l-1]] * (max_length-l))

def parse_files(dirname, initial_belief_states, num_prices, num_leave_probs, save_all):
    file_strategies = {}
    file_base = filebase(num_prices, num_leave_probs)
    print "parsing {0}".format(file_base)
    # print initial_belief_states
    for label, initial_belief_state in initial_belief_states.items():
        start = get_max_starting_strategy(dirname, file_base, initial_belief_state)
        print "best starting strategy is {0}".format(start)
        if save_all:
            file_strategies[label] = get_list_of_actions_for_multiple_pgs(dirname, file_base, start)
        else:
            file_strategies[label] = get_list_of_actions_for_single_pg(dirname, file_base, start)
    return file_strategies

def frange(x, y, jump):
  r = []
  while x <= y:
    r.append(x)
    x += jump
    x = round(x, 4)
  return r

def filebase(num_prices, num_leave_probs):
    return 'p-{0}-l-{1}'.format(num_prices, num_leave_probs)

def get_states(num_prices, num_leave_probs):
    prices = range(0, num_prices)
    leaves = frange(0.0, 1.0, 1.0/num_leave_probs)
    return itertools.product(prices, leaves)

def create_uniform_belief_dist(num_prices, num_leave_probs):
    belief_states = {}
    for p in frange(0.0, 1.0, 1.0/num_leave_probs):
        bs = []
        states = get_states(num_prices, num_leave_probs)
        for s in states:
            if s[1] == p:
                bs.append(1.0 / num_prices)
            else:
                bs.append(0)
        bs.append(0)
        belief_states[p] = bs
    return belief_states

def create_increasing_belief_dist(num_prices, num_leave_probs):
    return 0

# Usage: python GraphConsumer [-solve]
# -solve means it will also create and solve the pomdp in addition to graphing
def main():
    # Handle arguments
    solve = False
    if sys.argv[1] == '-solve':
        dirname = str(os.getpid())
        os.mkdir(dirname)
        solve = True
    else:
        dirname = sys.argv[1]

    # Set parameters
    num_prices = 8
    num_leave_probs = 20
    belief_dist = 'uniform'
    epsilon = .000000001
    horizon = 100
    save_all = False

    # Create the initial belief state based on
    if belief_dist == 'uniform':
        belief_states = create_uniform_belief_dist(num_prices, num_leave_probs)
    else:
        belief_states = create_uniform_belief_dist(num_prices, num_leave_probs)


    # Create and Solve the POMDP if requested
    if solve:
        solve_pomdp(dirname, num_prices, num_leave_probs, epsilon, horizon, save_all)

    # Parse, print, and graph the strategies
    pp = pprint.PrettyPrinter(indent=4)
    file_strategies = parse_files(dirname, belief_states, num_prices, num_leave_probs, save_all)
    straighten_list(file_strategies)
    print "pre-transform strategies:"
    pp.pprint(file_strategies)
    graph_strategies = transform_list(file_strategies)
    print "post-transform strategies:"
    pp.pprint(graph_strategies)
    marker = itertools.cycle((',', '+', '.', 'o', '*'))
    fig = plt.figure()
    for i in range(0, len(graph_strategies)):
        data = zip(*(graph_strategies[i]))
        ax = fig.add_subplot(111)
        plt.scatter(*data, marker = marker.next())
        ax.plot(*data)
        ax.set_title('BASIC: NumPrices: {0} NumLeaveProbs: {1}'.format(num_prices, num_leave_probs))
        ax.set_xlabel('Leaving Probability')
        ax.set_ylabel('Price')
        ax.set_ylim([0, num_prices + 1])
    plt.savefig(os.path.join(dirname, dirname + '.png'), format = 'png')


if __name__ == '__main__':
    main()
