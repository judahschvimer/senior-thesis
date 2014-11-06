import sys
import glob
import os
import numpy
import matplotlib.pyplot as plt

def get_max_strategy_in_alpha(filename):
    with open(filename, 'r') as f:
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

def get_list_of_actions(filename, start):
    with open(filename, 'r') as f:
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



def parse_files(initial_belief_state):
    file_strategies = {}
    for filename in glob.glob('*.alpha'):
        file_strategies[(os.path.splitext(filename)[0])] = get_max_strategy_in_alpha(filename)
    for filename in glob.glob('*.pg'):
        start = file_strategies[(os.path.splitext(filename)[0])]
        get_list_of_actions(filename, start)


def main():
    num_prices = 5
    uniform_belief_state = range(0, 1, 1.0/num_prices)
    parse_files(uniform_belief_state)



if __name__ == '__main__':
    main()
