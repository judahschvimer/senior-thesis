import sys
import itertools

def print_transition(f, action, start_state, end_state, probability):
    f.write('T : {0} : {1} : {2} {3}\n'.format(action, start_state, end_state, probability))

def print_observation(f, action, end_state, observation, probability):
    f.write('O : {0} : {1} : {2} {3}\n'.format(action, end_state, observation, probability))

def print_reward(f, action, start_state, end_state, observation, probability):
    f.write('R : {0} : {1} : {2} : {3} {4}\n'.format(action, start_state, end_state, observation, probability))

def wtp(p):
    return 'wtp-' + str(p)

def state(p, l):
    return 's-p{0}-l{1}'.format(p, str(l).replace('.', '_'))

def price(p):
    return 'p-' + str(p)

# Inputs: a willingness to pay and a price
# Outputs: probability of leaving given the wtp and price
def leave_function(price, wtp, leave_probability):
    if price <= wtp:
        return 0
    else:
        return leave_probability

def write_header(f, discount, values, prices, wtps, leaves):
    f.write('discount: {0}\n'.format(discount))
    f.write('values: {0}\n'.format(values))
    f.write('states: {0} done\n'.format(' '.join([state(pl[0], pl[1]) for pl in itertools.product(wtps, leaves)])))
    f.write('actions: {0}\n'.format(' '.join([price(p) for p in prices])))
    f.write('observations: o-1 o-2\n')
    f.write('\n')

    f.write('start include: {0}\n'.format(' '.join([state(pl[0], pl[1]) for pl in itertools.product(wtps, leaves)])))
    f.write('\n')

def write_transitions(f, prices, wtps, leaves):
    print_transition(f, '*', 'done', 'done', 1.0)
    f.write('\n')
    for p in prices:
        for l in leaves:
            for w in wtps:
                if p <= w:
                    print_transition(f, price(p), state(w, l), 'done', 1.0)
                else:
                    trans_prob = leave_function(p, w, l)
                    print_transition(f, price(p), state(w, l), state(w, l), 1.0 - trans_prob)
                    print_transition(f, price(p), state(w, l), 'done', trans_prob)
            f.write('\n')

def write_observations(f, prices, wtps, leaves):
    print_observation(f, '*', '*', 'o-1', 1.0)
    f.write('\n')

def write_rewards(f, prices, wtps, leaves):
    for p in prices:
        for l in leaves:
            for w in wtps:
                if p <= w:
                    print_reward(f, price(p), state(w, l), 'done', '*', float(p))
            f.write('\n')

def frange(x, y, jump):
  r = []
  while x <= y:
    r.append(x)
    x += jump
    x = round(x, 4)
  return r

def write_pomdp(out_file_name, discount, num_prices, values, num_leave_probs):
    wtps = range(0, num_prices)
    prices = range(0, num_prices)
    leaves = frange(0.0, 1.0, 1.0/num_leave_probs)
    with open(out_file_name, 'w') as f:
        write_header(f, discount, values, prices, wtps, leaves)
        write_transitions(f, prices, wtps, leaves)
        write_observations(f, prices, wtps, leaves)
        write_rewards(f, prices, wtps, leaves)

def main():
    out_file_name = 'Test.POMDP'
    discount = 0.95
    num_prices = 5
    values = 'reward'
    num_leave_probs = 4

    write_pomdp(out_file_name, discount, num_prices, values, num_leave_probs)

if __name__ == '__main__':
    main()