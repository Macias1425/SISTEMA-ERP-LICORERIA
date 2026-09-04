import {
  Banknote,
  Ban,
  Check,
  ClipboardList,
  CreditCard,
  Eye,
  Hand,
  LayoutGrid,
  LayoutList,
  Minus,
  Package,
  Plus,
  Receipt,
  Search,
  ShoppingCart,
  Trash2,
  TrendingDown,
  Truck,
  User,
  Wallet,
  Wine,
  X,
} from 'lucide-react';

const ICONS = {
  banknote: Banknote,
  ban: Ban,
  check: Check,
  clipboard: ClipboardList,
  creditCard: CreditCard,
  eye: Eye,
  grid: LayoutGrid,
  list: LayoutList,
  hand: Hand,
  minus: Minus,
  package: Package,
  plus: Plus,
  receipt: Receipt,
  search: Search,
  cart: ShoppingCart,
  trash: Trash2,
  trendDown: TrendingDown,
  truck: Truck,
  user: User,
  wallet: Wallet,
  wine: Wine,
  x: X,
};

export default function Icon({ name, size = 20, strokeWidth = 2, className = '', ...props }) {
  const Component = ICONS[name];
  if (!Component) {
    return null;
  }
  return (
    <Component
      size={size}
      strokeWidth={strokeWidth}
      className={`ui-icon${className ? ` ${className}` : ''}`}
      aria-hidden={props['aria-label'] ? undefined : true}
      {...props}
    />
  );
}
