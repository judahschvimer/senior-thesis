import sys

def write_header(f, discount, num_prices, values, wtps, prices):
    f.write('discount = {0}\n'.format(discount))
    f.write('values = {0}\n'.format(values))
    f.write('states: {0} done\n'.format(' '.join(wtps)))
    f.write('actions: {0}\n'.format(' '.join(prices)))
    f.write('observations: o-1 o-2\n')
    f.write('\n')

    f.write('start include: {0}\n'.format(' '.join(wtps)))
    f.write('\n')

def write_transition(action, start_state, end_state, probability):
    f.write('T : {0} : {1} : {2} {3}'.format(action, start_state, end_state, probability)

def write_observation(action, end_state, observation, probability):
    f.write('O : {0} : {1} : {2} {3}'.format(action, end_state, observation, probability)

def write_result(action, start_state, end_state, observation, probability):
    f.write('R : {0} : {1} : {2} : {3} {4}'.format(action, start_state, end_state, observation, probability)

def write_transitions(f, wtps, prices):
    f.write('T : * : done : done : 1.0')
    f.write('\n')



# Inputs: a willingness to pay and a price
# Outputs: probability of leaving given the wtp and price
def leave_function(wtp, price):
    leave_probability = 0.2

    if price <= wtp:
        return 0
    else:
        return leave_probability

def write_pomdp(out_file_name, discount, num_prices, values, wtps, prices):
    with open(out_file_name, 'w') as f:
        write_header(f, discount, num_prices, values, wtps, prices)
        write_transitions(f, wtps, prices)

def main():
    out_file_name = 'SolveConsumer.POMDP'
    discount = 0.95
    num_prices = 5
    values = 'reward'
    wtps = ['wtp-' + str(s) for s in range(1, num_prices + 1)]
    prices = ['p-' + str(s) for s in range(1, num_prices + 1)]

    write_pomdp(out_file_name, discount, num_prices, values, wtps, prices)

if __name__ == '__main__':
    main()