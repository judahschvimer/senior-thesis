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

import CreateLinear

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

def solve_pomdp(dirname, leave_slope, leave_intercept, num_prices, epsilon, horizon, save_all):
    discount = 0.95
    values = 'reward'
    file_base = filebase(leave_slope)
    file_name = os.path.join(dirname, file_base + '.POMDP')

    CreateLinear.write_pomdp(file_name, discount, num_prices, values, leave_slope, leave_intercept)
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

def parse_files(dirname, top_slope, step, initial_belief_state, num_prices, save_all):
    file_strategies = {}
    for p in frange(0.00, top_slope, step):
        file_base = filebase(p)
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
    x = round(x, 4)
  return r

def filebase(leave_probability):
    return 'p-{0}'.format(leave_probability)

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
    num_steps = 20
    leave_intercept = 0.8
    top_slope = (1.0-leave_intercept) / num_prices
    step = (top_slope / num_steps)
    belief_dist = 'uniform'
    epsilon = .0001
    horizon = 25
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
        for p in frange(0.0, top_slope, step):
            print p
            solve_pomdp(dirname, p, leave_intercept, num_prices, epsilon, horizon, save_all)

    # Parse, print, and graph the strategies
    pp = pprint.PrettyPrinter(indent=4)
    file_strategies = parse_files(dirname, top_slope, step, belief_state, num_prices, save_all)
    print "pre-transform strategies:"
    pp.pprint(file_strategies)
    graph_strategies = transform_list(file_strategies)
    print "post-transform strategies:"
    #pp.pprint(graph_strategies)
    marker = itertools.cycle((',', '+', '.', 'o', '*'))
    fig = plt.figure()
    for i in range(0, len(graph_strategies)):
        data = zip(*(graph_strategies[i]))
        ax = fig.add_subplot(111)
        plt.scatter(*data, marker = marker.next())
        ax.plot(*data)
        ax.set_title('LINEAR: NumPrices: {0} Intercept: {1} Slope Step: {2}'.format(num_prices, leave_intercept, step))
        ax.set_xlabel('Leaving Slope')
        ax.set_ylabel('Price')
        ax.set_ylim([0, num_prices + 1])
    plt.savefig(os.path.join(dirname, dirname + '.png'), format = 'png')


if __name__ == '__main__':
    main()
