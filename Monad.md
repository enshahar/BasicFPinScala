# 스칼라 초중급입자를 위한 모나드 해설

함수언어, 특히 하스켈과 같은 순수함수언어 계열을 다룰때는 반드시 마주체기 되며,
스칼라와 같은 하이브리드 언어를 사용하더라도 한번쯤은 관심을 가지게 되는 괴물(?)이 있으니 
바로 _모나드(Monad)_ 이다.

모나드에 대해 설명하는 튜토리얼을 들여다 보더라도 이해하기 쉽지가 않다. 
물론 대부분 영어로 되어있기 때문이라는게 첫번째 이유이고, 
두번쨰 이유는 추상적인 모나드 구조와 실제 우리가 코딩하는 코딩의 요소들 사이의 관계에 대해 
자세히 풀어 설명해주는 글이 드물다는 것이 두번쨰 이유이다. 

물론 스칼라에서 `map`과 `flatMap`이 정의된 클래스들을 `for`와 `yield`라는 
편리문법(syntactic sugar)과 결합해서 편리하게 다루는 것을 이미 본 사람이라면 
모나드가 뭔가 좋은일(?)을 하는 것이라는 정도는 알고 있지만, 
왜 대체 모나딕 룰이 필요한 건지, 대체 `flapMap`과 `map`이 둘 다 필요한 이유는 무엇인지 등등이 
궁금할 것이다.

나 또한 모나드를 100% 이해했다고는 할 수 없지만, 그간 들여다본 것을 바탕을
나름대로 모나드에 대해 설명해 보고자 한다. 독자 여러분도 이 글을 100% 신뢰하지 말고 
비판적으로 읽기 바란다.


## 값을 감싼 타입 만들기: 생성 함수(constructor)

기본적으로는 어떤 값을 감싼 다른 타입을 만드는 것에서 이 모든 일이 비롯된 것이다.
어떤 값을 감싸는 이유는 외부에 대해 그 값을 감추거나(정보은닉), 
감싸면서 어떤 정보를 추가하거나, 추가적인 연산을 정의하기 위한 것이다.

스칼라에서 어떤 값을 감싼 타입을 만드는 가장 쉬운 방법은 케이스 클래스를 활용하는 것이다.

아래 코드는 단순히 `T` 타입의 값을 감싼 타입인 `Boxed[T]`로 바꿔주는 케이스 클래스를 정의한다.

```
case class Boxed[T](value:T);
```

물론 이 클래스는 별로 유용하지는 않다. 조금 더 유용한 클래스를 만든다면, 스칼라의 `Option` 클래스와 
같은 것을 들 수 있다. `Option`이라는 이름이 의미하듯, 이 클래스는 어떤 값이 존재하거나(이때는 
`Some(value)`라는 타입이 된다), 어떤 값이 존재하지 않는(이때는 `None`이라는 하위 타입이 된다)
스칼라에서는 이런 경우 추상 클래스와 이를 상속한 하위 클래스들로 구현하게 된다.
이름 혼동을 막기 위해 `MyOption`이라는 이름을 사용한다.

```
abstract class MyOption[+A]

case class MySome[+A](x: A) extends MyOption[A]

case object MyNone extends MyOption[Nothing] 
```

또 다른 예로는 나중에 수행할 계산을 보관하기 위한 `Lazy`라는 이름의 클래스를 들 수 있다.

```
class Lazy[T](value: =>T) {
   def getValue():T = value 
}

object Lazy {
   def apply[T](v: =>T):Lazy[T] = 
   {
     new Lazy(v)
   }
}
```

다른 형태로는 컬렉션이 있다. 기본적 컬렉션의 하나인 리스트를 예로 들어보자.

```
abstract class MyList[+A] 

case class Cons[B](var hd: B, var tl: MyList[B]) extends MyList[B]

case object MyNil extends MyList[Nothing]
```

`Cons`는 머리(`hd`, 어떤 타입의 값)와 꼬리(`tl`, 리스트)로 이루어진 리스트이고, 
MyNil은 리스트의 끝(또는 아무 원소도 없는 리스트)을 표기하는 특별한 리스트이다. 
앞의 `MyOption`이나 `Lazy`등과 달리 여기서는 `MyList`가 재귀적으로 정의된다.

만약 위 정의만으로 정수 리스트 `MyList(1,2,3,4)`를 정의하려면 
```
val x:MyList[Int] = Cons(1,Cons(2,Cons(3,MyNil)))
```
과 같이 해야 한다. 

위의 모든 클래스의 공통점은 무엇일까? 
그것은 바로...

_공통점1: 어떤 타입 T에대해 새 클래스의 객체 C[T]를 만드는 생성자 C()가 존재한다._

라는 것이다. 이 조건은 새로운 객체를 생성하기 위해서는 반드시 필요한 조건이다. 


## "꺼내서 변환후 다시 묶기"

프로그래밍은 입력에서 출력까지의 일련의 변환 과정이며, 결국 함수의 조합이라 할 수 있다.
일단은 타입이 바뀌는 경우는 제외하고, 편의를 위해 같은 타입 안에서 일어나는 변환(
당연히 이런 경우에는 타입은 안 바뀌고 값만 바뀐다)만을 살펴보자. 

임의의 `T`에 대해 다루기 보다 가장 단순한 `Int`에 대해 생각해 보자.

어떤 수를 제곱하는 함수를 생각해 보자.

```
def squre(x:Int):Int = x * x
```

이제 앞에서 다뤘던 클래스 C에 대해 `C[Int]` 타입의 값 `v`에 대해 `square`를 적용해  
새로운 `C[Int]` 값을 만들어 내는 함수를 생각해 보자. 


```
def squareBoxed(x:Boxed[Int]):Boxed[Int] = {
  val v = x.value
  val new_v = square(v)
  Boxed(new_v)
}
```

`Boxed[Int]`에 대해 정의하는 것은 어렵지 않다. 박스에서 값을 꺼내서 `square`를 적용한 다음,
다시 이를 박스에 포장하면 된다. 설명을 위해 가능한 한 중간단계의 함수들을 서로 합성하지 않고 
명시적으로 값을 만들어 사용했다.

```
def squareMyOption(x:MyOption[Int]):MyOption[Int] = x match {
  case MySome(v) => {
    val new_v = square(v)
    MySome(new_v)
  }
  case MyNone => MyNone
}
```

옵션의 경우 특별히 신경쓸 부분은 `MyNone`인 경우 `square`를 할 수 없다는 것이다.
이 부분은 어쩔 수 없이 _특별히 따로 처리해야_ 한다. 

`Boxed[Int]`의 경우 클래스의 필드에서 값을 꺼냈지만(`v = x.value`) 여기서는 패턴매칭을 
사용해 `MySome`인 경우에는 `v`에 값을 바인드했다는 것에 유의하라. 물론 값을 가져온다는 
점에서는 패턴매칭이나 필드값을 사용하는 것이나 동일한 결과를 가져온다.

`squareLazy`의 정의는 어떻게 해야 할까? 처음에는 아마 다음과 같이 할 것이다.

```
def squareLazyInvlid(x:Lazy[Int]):Lazy[Int] = {
   val v = x.getValue();
   val new_v = square(v) 
   Lazy(new_v)
}
```
문제는 이렇게 하면 `Lazy`의 잇점이 없다는 것이다. 예를 들어 
```
scala> val x = Lazy({print("Lazy\n");10})
x: Lazy[Int] = Lazy@3c750bcb

scala> squareLazy(x)
Lazy
res17: Lazy[Int] = Lazy@62543f11
```

`squareLazy(x)`가 호출된 순간 값을 계산해 버린다. 이럴거면 그냥 `Int`를 쓰느니만 못하다.
따라서, 계산이 이 함수 호출시 일어나지 않고 나중에 일어나도록 _조심스럽게_ 정의할 필요가 있다.

테스트를 위해 `square`를 아래와 같이 다시 정의하자.
```
def square(x:Int):Int = { print("Square("+x+")\n"); x*x }
```

그 후 _조심스레_ squareLazy를 정의할 수 있다.

```
def squareLazy(x:Lazy[Int]):Lazy[Int] = {
   def v = x.getValue();
   def new_v = square(v) 
   Lazy(new_v)
}
```

이제 테스트를 해보자.
```
scala> val x = Lazy({print("Lazy\n");8})
x: Lazy[Int] = Lazy@1f6bb14e

scala> val s = squareLazy(x)
s: Lazy[Int] = Lazy@6c6cef3a

scala> s.getValue
Lazy
Square(8)
res27: Int = 64
```

실제 `getValue`를 호출해 값을 얻기 전까지는 계산이 지연됨을 확인할 수 있다.

리스트의 경우 재귀적 정의에 맞춰 결과 리스트를 만들때도 재귀적으로 해줘야 한다는 점이 
다른 점이다.

```
def squareMyList(x:MyList[Int]):MyList[Int] = x match {
  case Cons(v,t) => {
    val new_v = square(v)
    Cons(new_v, squareMyList(t))
  }
  case MyNil => MyNil
}
```

테스트를 해보면(앞에서 변경한 `square`가 쓰여서 계산 단계가 표시된다) 다음과 같다.

```
scala> squareMyList(Cons(1,Cons(2,MyNil)))
Square(1)
Square(2)
res28: MyList[Int] = Cons(1,Cons(4,MyNil))
```

우리는 `Int`가 있고, `Int=>Int` 타입의 함수가 있을 때, 
`C[Int]=>C[Int]`타입의 함수를 만들었다. 각 클래스에 대해 수행했던 작업을 잘 생각해 보면, 
다음과 같은 공통점을 뽑아낼 수 있다.

_공통점2: 어떤 타입 C[T]에대해 내부 값을 알아낼 수 있는 방법이 존재한다._

_공통점3: T=>T타입 함수가 있을 때, 이를 이용한 C[T]=>C[T] 타입의 변환 함수는 C[T]의 의미에 따라 각각 다른 방식으로 조심스럽게 정의된다._


## "꺼내서 변환후 다시 묶기"의 일반화: `map`

앞의 각 구현을 보면 다음과 같은 일반적 요소를 뽑아낼 수 있다.

1. `C[Int]` 타입의 감싸진 값 `x`가 있다.
2. `Int=>Int` 타입의 함수 `f`가 있다.
3. `C[Int]`안에서는 `x`에서 내부의 값(`v`)을 뽑아내 `C[Int]`의 의미에 적합하게 `f`를 적용해서 `new_v`를 얻는다.
4. `C[Int]`의 생성자를 이용해 `new_v`를 감싸서 반환한다.

이제 함수의 타입을 일반화하고(`Int`->`Int` 함수를 일반적인 함수 타입 `T=>R`로 변경) `squareXXX` 함수에 넘기고,
`squareMyList`등의 이름을 `mapXXX()`라고 변경하자. 

```
def mapBoxed[T,R](f:T=>R) = (x:Boxed[T]) => {
  val v = x.value
  val new_v = f(v)
  Boxed(new_v)
}
```

당연히 `squareBoxed`는 이 함수를 적용해 만들어낼 수 있다.


```
val squareBoxed = mapBoxed(square)
```

부연설명을 조금 하자면, 커링/언커링 관계를 활용하면 다음과 같이 정의해도 마찬가지라 할 수 있다.
(스칼라 메소드와 함수 타입에 대해 조금 더 이해를 해야 하지만, 자세한 설명은 생략한다.
 영어로 되어있지만, [스택오버플로](http://stackoverflow.com/questions/2529184/difference-between-method-and-function-in-scala)나 
 [이 블로그글](http://jim-mcbeath.blogspot.com.au/2009/05/scala-functions-vs-methods.html)을 참조하라.)

```
def mapBoxedUncurried[T,R](f:T=>R, x:Boxed[T]) = {
  val v = x.value
  val new_v = f(v)
  Boxed(new_v)
}

def squareBoxed2(x:Boxed[Int]) = mapBoxedUncurried(square,x)

val squareBoxed3 = mapBoxedUncurried(square,_:Boxed[Int])
```


다른 클래스에 대해서도 마찬가지로 정리해 보자.

```
// 옵션
def mapMyOption[T,R](f:T=>R) = (x:MyOption[T]) => x match {
  case MySome(v) => {
    val new_v = f(v)
    MySome(new_v)
  }
  case MyNone => MyNone
}

// 지연값
def mapLazy[T,R](f:T=>R) = (x:Lazy[T]) => {
   def v = x.getValue();
   def new_v = f(v) 
   Lazy(new_v)
}

// 리스트
def mapMyList[T,R](f:T=>R):MyList[T]=>MyList[R] = (x:MyList[T]) => x match {
  case Cons(v,t) => {
    val new_v = f(v)
    Cons(new_v, mapMyList(f)(t))
  }
  case MyNil => MyNil
}

```

공통된 패턴이 보일 것이다. 지금까지 본 공통점을 정리하고, _공통점 3_ 을 더 일반화 하면 다음과 같다.

_공통점1: 어떤 타입 `T`에대해 새 클래스의 객체 `C[T]`를 만드는 생성자 `C()`가 존재한다._

_공통점2: 어떤 타입 `C[T]`에대해 내부 값을 알아낼 수 있는 방법이 존재한다._

_공통점3: `T=>R` 타입 함수 `f`가 있을 때, 이를 이용해 `C[T]=>C[R]` 타입의 변환을 수행하는 `map` 함수는 `C[T]`의 의미에 따라 각각 다른 방식으로 조심스럽게 정의된다._


## 

이제 `T=>R` 타입의 `f`와 `R=>S` 타입의 `g`가 있다고 하자.

다음과 같은 경우를 예로 들 수 있을 것이다.

```
def f(x:Int):(Int,Double) = (x,Math.log(x))

def g(x:(Int,Double)):String = "log(" + x._1 + ") = " + x._2
```

이제, 두 함수를 합성 할 수 있다.

```
def gof(x:Int) = g(f(x))
```


앞에서 정의한 `map`이 있다면, `C[Int]`에서 `C[(Int,Double)]`로 가는 함수와 
`C[(Int,Double)]`에서 `C[String]`으로 가는 함수는 쉽게 만들 수 있다.
가능하다면 이 두 함수를 합성해 `C[Int]`에서 `C[String]`으로 가는 
함수를 만들 수 있다면 좋을 것이다.


`Boxed`를 예로 살펴보자.

```
def mapFBoxed(x:Boxed[Int]):Boxed[(Int,Double)] = mapBoxed(f)(x)

def mapGBoxed(x:Boxed[(Int,Double)]):Boxed[String] = mapBoxed(g)(x)

def mapGBoxedOmapFBoxed(x:Boxed[Int]):Boxed[String] = mapGBoxed(mapFBoxed(x))
```

그런데, 우리가 `gof`를 가지고 있으므로, 다음과 같이 `mapGOFBoxed`를 정의할 수도 있다.
```
def mapGOFBoxed(x:Boxed[Int]):Boxed[String] = mapBoxed(gof)(x)
```

