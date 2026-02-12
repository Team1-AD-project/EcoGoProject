import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import {
  Leaf,
  TrendingUp,
  Users,
  Trophy,
  Gift,
  Shield,
  Zap,
  Heart,
  MapPin,
  Award,
  Star,
  ArrowRight,
  Check
} from 'lucide-react';

export function LandingPage() {
  return (
    <div className="min-h-screen bg-white">
      {/* Navigation */}
      <nav className="fixed top-0 left-0 right-0 bg-white/90 backdrop-blur-sm border-b z-50">
        <div className="max-w-7xl mx-auto px-6 py-4 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="size-10 bg-gradient-to-br from-green-600 to-blue-600 rounded-xl flex items-center justify-center">
              <Leaf className="size-6 text-white" />
            </div>
            <span className="text-xl font-bold text-gray-900">Ecogo</span>
          </div>
          <div className="hidden md:flex items-center gap-8">
            <a href="#features" className="text-gray-600 hover:text-gray-900">Features</a>
            <a href="#benefits" className="text-gray-600 hover:text-gray-900">Benefits</a>
            <a href="#about" className="text-gray-600 hover:text-gray-900">About</a>
            <Button
              className="bg-gradient-to-r from-green-600 to-blue-600 hover:from-green-700 hover:to-blue-700 text-white"
            >
              Download App
            </Button>
          </div>
          <Button
            size="sm"
            className="md:hidden bg-gradient-to-r from-green-600 to-blue-600 text-white"
          >
            Download
          </Button>
        </div>
      </nav>

      {/* Hero Section */}
      <section className="pt-32 pb-20 px-6">
        <div className="max-w-7xl mx-auto text-center">
          <Badge className="mb-6 bg-green-100 text-green-700 hover:bg-green-100">
            <Zap className="size-3 mr-1" />
            Eco-Friendly Transportation Platform
          </Badge>
          <h1 className="text-5xl md:text-6xl font-bold text-gray-900 mb-6">
            Walk More,
            <br />
            <span className="bg-gradient-to-r from-green-600 to-blue-600 bg-clip-text text-transparent">
              Earn Rewards
            </span>
          </h1>
          <p className="text-xl text-gray-600 mb-8 max-w-2xl mx-auto">
            Join our community of eco-conscious users. Track your steps, reduce carbon footprint, and earn points for sustainable transportation choices.
          </p>
          <div className="flex flex-wrap gap-4 justify-center">
            <Button
              size="lg"
              className="bg-gradient-to-r from-green-600 to-blue-600 hover:from-green-700 hover:to-blue-700 text-white gap-2"
            >
              Get Started
              <ArrowRight className="size-5" />
            </Button>
            <Button
              size="lg"
              variant="outline"
              className="border-2"
            >
              Learn More
            </Button>
          </div>

          {/* Stats */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-6 mt-16 max-w-4xl mx-auto">
            <div>
              <p className="text-3xl font-bold text-gray-900">50K+</p>
              <p className="text-sm text-gray-600">Active Users</p>
            </div>
            <div>
              <p className="text-3xl font-bold text-gray-900">2.5M</p>
              <p className="text-sm text-gray-600">Steps Today</p>
            </div>
            <div>
              <p className="text-3xl font-bold text-gray-900">1,234</p>
              <p className="text-sm text-gray-600">Tons CO₂ Saved</p>
            </div>
            <div>
              <p className="text-3xl font-bold text-gray-900">15K</p>
              <p className="text-sm text-gray-600">Rewards Claimed</p>
            </div>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section id="features" className="py-20 px-6 bg-gray-50">
        <div className="max-w-7xl mx-auto">
          <div className="text-center mb-16">
            <h2 className="text-4xl font-bold text-gray-900 mb-4">
              Platform Features
            </h2>
            <p className="text-lg text-gray-600 max-w-2xl mx-auto">
              Comprehensive tools to track, manage, and reward eco-friendly transportation
            </p>
          </div>

          <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
            <Card className="p-6 hover:shadow-lg transition-shadow">
              <div className="w-12 h-12 rounded-lg bg-green-100 flex items-center justify-center mb-4">
                <MapPin className="size-6 text-green-600" />
              </div>
              <h3 className="text-xl font-semibold text-gray-900 mb-2">Trip Tracking</h3>
              <p className="text-gray-600">
                Automatically track walking, cycling, and public transit trips with GPS integration.
              </p>
            </Card>

            <Card className="p-6 hover:shadow-lg transition-shadow">
              <div className="w-12 h-12 rounded-lg bg-blue-100 flex items-center justify-center mb-4">
                <Award className="size-6 text-blue-600" />
              </div>
              <h3 className="text-xl font-semibold text-gray-900 mb-2">Points System</h3>
              <p className="text-gray-600">
                Earn points for every eco-friendly mile. Redeem for rewards in our store.
              </p>
            </Card>

            <Card className="p-6 hover:shadow-lg transition-shadow">
              <div className="w-12 h-12 rounded-lg bg-purple-100 flex items-center justify-center mb-4">
                <Trophy className="size-6 text-purple-600" />
              </div>
              <h3 className="text-xl font-semibold text-gray-900 mb-2">Leaderboards</h3>
              <p className="text-gray-600">
                Compete with friends and community members on weekly step challenges.
              </p>
            </Card>

            <Card className="p-6 hover:shadow-lg transition-shadow">
              <div className="w-12 h-12 rounded-lg bg-yellow-100 flex items-center justify-center mb-4">
                <Gift className="size-6 text-yellow-600" />
              </div>
              <h3 className="text-xl font-semibold text-gray-900 mb-2">Reward Store</h3>
              <p className="text-gray-600">
                Redeem points for eco-products, discounts, and exclusive rewards.
              </p>
            </Card>

            <Card className="p-6 hover:shadow-lg transition-shadow">
              <div className="w-12 h-12 rounded-lg bg-pink-100 flex items-center justify-center mb-4">
                <Star className="size-6 text-pink-600" />
              </div>
              <h3 className="text-xl font-semibold text-gray-900 mb-2">Collectibles</h3>
              <p className="text-gray-600">
                Unlock badges and collect virtual pets as you achieve milestones.
              </p>
            </Card>

            <Card className="p-6 hover:shadow-lg transition-shadow">
              <div className="w-12 h-12 rounded-lg bg-indigo-100 flex items-center justify-center mb-4">
                <Shield className="size-6 text-indigo-600" />
              </div>
              <h3 className="text-xl font-semibold text-gray-900 mb-2">VIP Membership</h3>
              <p className="text-gray-600">
                Premium features, bonus points, and exclusive rewards for VIP members.
              </p>
            </Card>
          </div>
        </div>
      </section>

      {/* Benefits Section */}
      <section id="benefits" className="py-20 px-6">
        <div className="max-w-7xl mx-auto">
          <div className="grid md:grid-cols-2 gap-12 items-center">
            <div>
              <h2 className="text-4xl font-bold text-gray-900 mb-6">
                Why Choose Ecogo?
              </h2>
              <p className="text-lg text-gray-600 mb-8">
                Join thousands of users making a positive impact on the environment while improving their health.
              </p>

              <div className="space-y-4">
                <div className="flex items-start gap-3">
                  <div className="w-6 h-6 rounded-full bg-green-100 flex items-center justify-center flex-shrink-0 mt-0.5">
                    <Check className="size-4 text-green-600" />
                  </div>
                  <div>
                    <h3 className="font-semibold text-gray-900 mb-1">Reduce Carbon Footprint</h3>
                    <p className="text-gray-600">Track and minimize your environmental impact with every trip.</p>
                  </div>
                </div>

                <div className="flex items-start gap-3">
                  <div className="w-6 h-6 rounded-full bg-green-100 flex items-center justify-center flex-shrink-0 mt-0.5">
                    <Check className="size-4 text-green-600" />
                  </div>
                  <div>
                    <h3 className="font-semibold text-gray-900 mb-1">Health Benefits</h3>
                    <p className="text-gray-600">Improve fitness and wellbeing through active transportation.</p>
                  </div>
                </div>

                <div className="flex items-start gap-3">
                  <div className="w-6 h-6 rounded-full bg-green-100 flex items-center justify-center flex-shrink-0 mt-0.5">
                    <Check className="size-4 text-green-600" />
                  </div>
                  <div>
                    <h3 className="font-semibold text-gray-900 mb-1">Earn Real Rewards</h3>
                    <p className="text-gray-600">Get tangible benefits for your eco-friendly choices.</p>
                  </div>
                </div>

                <div className="flex items-start gap-3">
                  <div className="w-6 h-6 rounded-full bg-green-100 flex items-center justify-center flex-shrink-0 mt-0.5">
                    <Check className="size-4 text-green-600" />
                  </div>
                  <div>
                    <h3 className="font-semibold text-gray-900 mb-1">Community Engagement</h3>
                    <p className="text-gray-600">Connect with like-minded individuals and compete in challenges.</p>
                  </div>
                </div>
              </div>
            </div>

            <div className="relative">
              <div className="grid grid-cols-2 gap-4">
                <Card className="p-6 bg-gradient-to-br from-green-500 to-green-600 text-white">
                  <TrendingUp className="size-8 mb-3" />
                  <p className="text-2xl font-bold mb-1">+45%</p>
                  <p className="text-sm opacity-90">Increase in Walking</p>
                </Card>
                <Card className="p-6 bg-gradient-to-br from-blue-500 to-blue-600 text-white mt-8">
                  <Users className="size-8 mb-3" />
                  <p className="text-2xl font-bold mb-1">50K+</p>
                  <p className="text-sm opacity-90">Active Members</p>
                </Card>
                <Card className="p-6 bg-gradient-to-br from-purple-500 to-purple-600 text-white -mt-4">
                  <Heart className="size-8 mb-3" />
                  <p className="text-2xl font-bold mb-1">98%</p>
                  <p className="text-sm opacity-90">User Satisfaction</p>
                </Card>
                <Card className="p-6 bg-gradient-to-br from-yellow-500 to-yellow-600 text-white mt-4">
                  <Leaf className="size-8 mb-3" />
                  <p className="text-2xl font-bold mb-1">1.2K</p>
                  <p className="text-sm opacity-90">Tons CO₂ Saved</p>
                </Card>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-20 px-6 bg-gradient-to-br from-green-600 via-blue-600 to-purple-700">
        <div className="max-w-4xl mx-auto text-center text-white">
          <h2 className="text-4xl font-bold mb-4">
            Ready to Make a Difference?
          </h2>
          <p className="text-xl opacity-90 mb-8">
            Join our community today and start earning rewards for eco-friendly choices.
          </p>
          <Button
            size="lg"
            className="bg-white text-gray-900 hover:bg-gray-100"
          >
            Download App
          </Button>
        </div>
      </section>

      {/* Footer */}
      <footer id="about" className="bg-gray-900 text-gray-400 py-12 px-6">
        <div className="max-w-7xl mx-auto">
          <div className="grid md:grid-cols-4 gap-8 mb-8">
            <div>
              <div className="flex items-center gap-2 mb-4">
                <div className="w-8 h-8 rounded-full bg-gradient-to-br from-green-500 to-blue-600 flex items-center justify-center">
                  <Leaf className="size-5 text-white" />
                </div>
                <span className="text-lg font-bold text-white">Ecogo</span>
              </div>
              <p className="text-sm">
                Making the world a greener place, one step at a time.
              </p>
            </div>
            <div>
              <h4 className="text-white font-semibold mb-3">Product</h4>
              <ul className="space-y-2 text-sm">
                <li><a href="#" className="hover:text-white">Features</a></li>
                <li><a href="#" className="hover:text-white">Pricing</a></li>
                <li><a href="#" className="hover:text-white">FAQ</a></li>
              </ul>
            </div>
            <div>
              <h4 className="text-white font-semibold mb-3">Company</h4>
              <ul className="space-y-2 text-sm">
                <li><a href="#" className="hover:text-white">About</a></li>
                <li><a href="#" className="hover:text-white">Blog</a></li>
                <li><a href="#" className="hover:text-white">Careers</a></li>
              </ul>
            </div>
            <div>
              <h4 className="text-white font-semibold mb-3">Legal</h4>
              <ul className="space-y-2 text-sm">
                <li><a href="#" className="hover:text-white">Privacy</a></li>
                <li><a href="#" className="hover:text-white">Terms</a></li>
                <li><a href="#" className="hover:text-white">Contact</a></li>
              </ul>
            </div>
          </div>
          <div className="border-t border-gray-800 pt-8 text-center text-sm">
            <p>&copy; 2026 Ecogo. All rights reserved.</p>
          </div>
        </div>
      </footer>
    </div>
  );
}
