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

import CreateMovingWTP

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
            oldcurr = curr
            curr = int(lines[curr][2])
            if curr == 0 or oldcurr == curr:
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
                    oldcurr = curr
                    curr = int(lines[curr][2])
                    if curr == 0 or curr == oldcurr:
                        bargaining = False
                return actions

def solve_pomdp(dirname, leave_probability, move_wtp_probability, num_prices, epsilon, horizon, save_all):
    discount = 0.999
    values = 'reward'
    file_base = filebase(leave_probability, move_wtp_probability)
    file_name = os.path.join(dirname, file_base + '.POMDP')

    CreateMovingWTP.write_pomdp(file_name, discount, num_prices, values, leave_probability, move_wtp_probability)
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

def parse_files(dirname, leave_probability, move_probability_step, initial_belief_state, num_prices, save_all):
    file_strategies = {}
    for p in frange(0.00, 1.00, move_probability_step):
        file_base = filebase(leave_probability, p)
        print "parsing {0}".format(file_base)
        start = get_max_starting_strategy(dirname, file_base, initial_belief_state)
        print "best starting strategy is {0}".format(start)
        if save_all:
            file_strategies[p] = get_list_of_actions_for_multiple_pgs(dirname, file_base, start)
        else:
            file_strategies[p] = get_list_of_actions_for_single_pg(dirname, file_base, start)
    return file_strategies

def frange(x, y, jump):
  r = []
  while x <= y:
    r.append(x)
    x += jump
    x = round(x, 2)
  return r

def filebase(leave_probability, move_wtp_probability):
    return 'p-{0}-{1}'.format(leave_probability, move_wtp_probability)

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
    num_prices = 7
    leave_probability = 0.0
    move_probability_step = 0.05
    belief_dist = 'uniform'
    epsilon = 0.000000000000000001
    horizon = 25000
    save_all = False

    # Create the initial belief state based on
    if belief_dist == 'uniform':
        belief_state = [1.0/num_prices] * num_prices
        belief_state.append(0)
    else:
        belief_state = [1.0/num_prices] * num_prices
        belief_state.append(0)

    # Create and Solve the POMDP if requested
    if solve:
        for mp in frange(0.00, 1.00, move_probability_step):
            solve_pomdp(dirname, leave_probability, mp, num_prices, epsilon, horizon, save_all)

    # Parse, print, and graph the strategies
    pp = pprint.PrettyPrinter(indent=4)
    file_strategies = parse_files(dirname, leave_probability, move_probability_step, belief_state, num_prices, save_all)
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
        ax.set_title('MoveWTP: NumPrices: {0} Leave P: {1} Move Step: {2}'.format(num_prices, leave_probability, move_probability_step))
        ax.set_xlabel('WTP Moving Probability')
        ax.set_ylabel('Price')
        ax.set_ylim([0, num_prices + 1])
    plt.savefig(os.path.join(dirname, dirname + '.png'), format = 'png')


if __name__ == '__main__':
    main()
