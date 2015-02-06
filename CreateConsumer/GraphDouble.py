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
import scipy.misc
import scipy.special
import math

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
    leaves = frange(0.0, 1.0, 1.0/(num_leave_probs-1))
    return itertools.product(prices, leaves)

def create_price_uniform_belief_dist(num_prices, num_leave_probs):
    belief_states = {}
    for p in frange(0.0, 0.9999, 1.0/(num_leave_probs-1)):
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

def create_leave_uniform_belief_dist(num_prices, num_leave_probs):
    belief_states = {}
    for wtp in range(1, num_prices):
        bs = []
        states = get_states(num_prices, num_leave_probs)
        for s in states:
            if s[0] == wtp:
                bs.append(1.0 / num_leave_probs)
            else:
                bs.append(0)
        bs.append(0)
        belief_states[wtp] = bs
    return belief_states

def create_price_increasing_belief_dist(num_prices, num_leave_probs):
    belief_states = {}
    total_increments = (num_prices + 1) * (num_prices + 2) / 2
    increment_size = 1.0 / total_increments
    for p in frange(0.0, 0.9999, 1.0/(num_leave_probs-1)):
        bs = []
        states = get_states(num_prices, num_leave_probs)
        for s in states:
            if s[1] == p:
                bs.append(increment_size * (s[0] + 1))
            else:
                bs.append(0)
        bs.append(0)
        belief_states[p] = bs
    return belief_states

def create_price_decreasing_belief_dist(num_prices, num_leave_probs):
    belief_states = {}
    total_increments = (num_prices + 1) * (num_prices + 2) / 2
    increment_size = 1.0 / total_increments
    for p in frange(0.0, 0.9999, 1.0/(num_leave_probs-1)):
        bs = []
        states = get_states(num_prices, num_leave_probs)
        for s in states:
            if s[1] == p:
                bs.append(increment_size * (num_prices - s[0]))
            else:
                bs.append(0)
        bs.append(0)
        belief_states[p] = bs
    return belief_states

def create_price_binomial_belief_dist(num_prices, num_leave_probs, probability):
    belief_states = {}
    n = num_prices - 1
    for p in frange(0.0, 0.9999, 1.0/(num_leave_probs-1)):
        bs = []
        states = get_states(num_prices, num_leave_probs)
        for s in states:
            if s[1] == p:
                k = s[0]
                bs.append(scipy.misc.comb(n, k)*math.pow(probability, k)*(math.pow((1-probability),(n-k))))
            else:
                bs.append(0)
        bs.append(0)
        belief_states[p] = bs
    return belief_states

def create_price_poisson_belief_dist(num_prices, num_leave_probs, lam):
    belief_states = {}
    for p in frange(0.0, 0.9999, 1.0/(num_leave_probs-1)):
        bs = []
        states = get_states(num_prices, num_leave_probs)
        for s in states:
            if s[1] == p:
                k = s[0]
                bs.append(math.pow(lam, k) * math.exp(-1 * lam) / math.factorial(k))
            else:
                bs.append(0)
        bs.append(0)
        belief_states[p] = bs
    return belief_states

def create_price_beta_binomial_belief_dist(num_prices, num_leave_probs, a, b):
    belief_states = {}
    n = num_prices - 1
    for p in frange(0.0, 0.9999, 1.0 / (num_leave_probs - 1)):
        bs = []
        states = get_states(num_prices, num_leave_probs)
        for s in states:
            if s[1] == p:
                k = s[0]
                bs.append(scipy.misc.comb(n, k) * scipy.special.beta(k + a, n - k + b) / scipy.special.beta(a, b))
            else:
                bs.append(0)
        bs.append(0)
        belief_states[p] = bs
    return belief_states

def create_price_uniform_leave_increasing_belief_dist(num_prices, num_leave_probs):
    belief_states = {}
    n = num_leave_probs
    for base in range(0, 11):
        bs = []
        states = get_states(num_prices, num_leave_probs)
        for s in states:
            # use s[0] to determine the slope
            k = s[1] * num_leave_probs
            # If the WTP is in the bottom half skew it left, otherwise skew it right
            if s[0] < num_prices / 2:
                a = base + 2 * abs((num_prices / 2) - s[0])
                b = base
            else:
                b = base + 2 * abs((num_prices / 2) - s[0])
                a = base
            bs.append((scipy.misc.comb(n, k) * scipy.special.beta(k + a, n - k + b) / scipy.special.beta(a, b)) / num_prices)
        bs.append(0)
        belief_states[base] = bs
    #for l, s in belief_states.items():
        #print sum(s)
    return belief_states

def create_price_uniform_leave_decreasing_belief_dist(num_prices, num_leave_probs):
    belief_states = {}
    n = num_leave_probs
    for base in range(0, 11):
        bs = []
        states = get_states(num_prices, num_leave_probs)
        for s in states:
            # use s[0] to determine the slope
            k = s[1] * num_leave_probs
            # If the WTP is in the bottom half skew it left, otherwise skew it right
            if s[0] < num_prices / 2:
                b = base + 2 * abs((num_prices / 2) - s[0])
                a = base
            else:
                a = base + 2 * abs((num_prices / 2) - s[0])
                b = base
            bs.append((scipy.misc.comb(n, k) * scipy.special.beta(k + a, n - k + b) / scipy.special.beta(a, b)) / num_prices)
        bs.append(0)
        belief_states[base] = bs
    #for l, s in belief_states.items():
        #print sum(s)
    return belief_states



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
    num_leave_probs = 21
    belief_dist = 'price_uniform'
    epsilon = .000000001
    horizon = 1000
    save_all = False
    xlabel = "leave_probability"

    # Create the initial belief state based on
    if belief_dist == 'price_uniform':
        belief_states = create_price_uniform_belief_dist(num_prices, num_leave_probs)
    elif belief_dist == 'price_increasing':
        belief_states = create_price_increasing_belief_dist(num_prices, num_leave_probs)
    elif belief_dist == 'price_decreasing':
        belief_states = create_price_decreasing_belief_dist(num_prices, num_leave_probs)
    elif belief_dist == 'price_binomial':
        belief_states = create_price_binomial_belief_dist(num_prices, num_leave_probs, 0.5)
    elif belief_dist == 'price_poisson':
        belief_states = create_price_poisson_belief_dist(num_prices, num_leave_probs, 1)
    elif belief_dist == 'price_bimodal':
        belief_states = create_price_beta_binomial_belief_dist(num_prices, num_leave_probs, 0.3, 0.3)
    elif belief_dist == 'leave_uniform':
        belief_states = create_leave_uniform_belief_dist(num_prices, num_leave_probs)
        xlabel = "Constant WTP"
    elif belief_dist == 'price_uniform_leave_increasing':
        belief_states = create_price_uniform_leave_increasing_belief_dist(num_prices, num_leave_probs)
        xlabel = "Base Skewness"
    elif belief_dist == 'price_uniform_leave_decreasing':
        belief_states = create_price_uniform_leave_decreasing_belief_dist(num_prices, num_leave_probs)
        xlabel = "Base Skewness"
    else:
        belief_states = create_price_uniform_belief_dist(num_prices, num_leave_probs)


    # Create and Solve the POMDP if requested
    if solve:
        solve_pomdp(dirname, num_prices, num_leave_probs, epsilon, horizon, save_all)

    # Parse, print, and graph the strategies
    pp = pprint.PrettyPrinter(indent=4)
    print "belief states:"
    print(belief_states)
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
        ax.set_title('JOINT: Dist: {0} NumPrices: {1} NumLeaves: {2}'.format(belief_dist, num_prices, num_leave_probs))
        ax.set_xlabel(xlabel)
        ax.set_ylabel('Price')
        ax.set_ylim([0, num_prices + 1])
    plt.savefig(os.path.join(dirname, belief_dist + '.png'), format = 'png')


if __name__ == '__main__':
    main()
