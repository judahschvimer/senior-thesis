import sys
import glob
import os
import numpy
import matplotlib.pyplot as plt
import subprocess
import CreateConsumer

def get_max_starting_strategy(file_base, initial_belief_state):
    with open(file_base + '.alpha', 'r') as f:
        lines = f.readlines()
        max_sum = -99999
        max_strategy = -1
        for idx, line in lines:
            if idx % 3 == 2:
                weights = map(int, line.split(' '))
                sum = numpy.dot(weights, initial_belief_state)
                if sum > max_sum
                    max_strategy = (idx-2)/3
    return max_strategy

def get_list_of_actions(file_base, start):
    with open(file_base + '.pg', 'r') as f:
        lines = f.readlines()
        lines = map(split, lines)
        curr = start
        actions = []
        bargaining = True
        while bargaining:
            actions.append(lines[curr][1] + 1)
            curr = lines[curr][2]
            if curr == 0:
                bargaining = False

def solve_pomdp(leave_probability):
    discount = 0.95
    num_prices = 5
    values = 'reward'
    leave_probability = 0.2
    file_base = 'p-{0}'.format(leave_probability)
    CreateConsumer.write_pomdp(file_base + '.POMDP', discount, num_prices, values, leave_probability)
    subprocess.call(['../pomdp-solve-5.3/src/pomdp-solve', '-pomdp', file_base + '.POMDP'])
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
        index_lists[idx] = index_list
    return index_lists


def parse_files(initial_belief_state):
    file_strategies = {}
    for p in range(0, 1, 1):
        file_base = solve_pomdp(p)
        start = get_max_starting_strategy(file_base initial_belief_state)
        file_strategies[p] = get_list_of_actions(file_base, start)
    return file_strategies

def main():
    num_prices = 5
    uniform_belief_state = range(0, 1, 1.0/num_prices)
    file_strategies = parse_files(uniform_belief_state)
    graph_strategies = transform_list(file_strategies)
    marker = itertools.cycle((',', '+', '.', 'o', '*'))
    for i in range(0, len(graph_strategies)):
        data = zip(*(graph_strategies[i]))
        plt.scatter(*data, marker = marker.next())
        plt.plot(*data)
    plt.show()


if __name__ == '__main__':
    main()
