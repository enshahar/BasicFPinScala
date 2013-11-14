// 단순히 감싸기만 한 클래스
case class Boxed[T](value:T);

// 값이 정상이면 MySome(값), 값이 비정상이면  MyNone인 클래스
abstract class MyOption[+A]
case class MySome[+A](x: A) extends MyOption[A]
case object MyNone extends MyOption[Nothing] 

// 지연된 계산을 저장함
class Lazy[T](value: =>T) {
   def getValue():T = value 
}

object Lazy {
   def apply[T](v: =>T):Lazy[T] = 
   {
     new Lazy(v)
   }
}

// 리스트
abstract class MyList[+A] 
case class Cons[B](var hd: B, var tl: MyList[B]) extends MyList[B]
case object MyNil extends MyList[Nothing]

// 리스트 만들기 테스트
val x:MyList[Int] = Cons(1,Cons(2,Cons(3,MyNil)))

// square(제곱구하기) 함수. 호출 여부를 알기 위해 print문을 넣어둠.
def square(x:Int):Int = { print("Square("+x+")\n"); x*x }

// Boxed에 대해 square하기
def squareBoxed(x:Boxed[Int]):Boxed[Int] = {
  val v = x.value
  val new_v = square(v)
  Boxed(new_v)
}

// MyOption에 대해 square하기
def squareMyOption(x:MyOption[Int]):MyOption[Int] = x match {
  case MySome(v) => {
    val new_v = square(v)
    MySome(new_v)
  }
  case MyNone => MyNone
}

// Lazy에 대해 square하기(잘못된 구현)
def squareLazyInvalid(x:Lazy[Int]):Lazy[Int] = {
   val v = x.getValue();
   val new_v = square(v) 
   Lazy(new_v)
}

// Lazy에 대해 square하기(lazy의 의미를 살린 구현)
def squareLazy(x:Lazy[Int]):Lazy[Int] = {
   def v = x.getValue();
   def new_v = square(v) 
   Lazy(new_v)
}

// 리스트에 대해 square하기
// 리스트에 대해 square한다는 말을 어떻게 정하느냐에 따라 다르지만,
// "모든 엘리먼트에 대해 square를 해서 다시 리스트로 만드는 과정"을 
// "리스트에 대해 square하기"라고 하기로 함
def squareMyList(x:MyList[Int]):MyList[Int] = x match {
  case Cons(v,t) => {
    val new_v = square(v)
    Cons(new_v, squareMyList(t))
  }
  case MyNil => MyNil
}

// Boxed에 대해 일반화. 커리된 버전
def mapBoxed[T,R](f:T=>R) = (x:Boxed[T]) => {
  val v = x.value
  val new_v = f(v)
  Boxed(new_v)
}

// squareBoxed를 위 mapBoxed를 사용해 구현
val squareBoxedUsingMap = mapBoxed(square)

// mapBoxed의 커리되지 않은 버전.
def mapBoxedUncurried[T,R](f:T=>R, x:Boxed[T]) = {
  val v = x.value
  val new_v = f(v)
  Boxed(new_v)
}

// squareBoxed를 커리되지 않은 맵을 사용해 만들기
def squareBoxedUsingUncurriedMap(x:Boxed[Int]) = mapBoxedUncurried(square,x)

// 커리되지 않은 맵을 사용하되 val로 만들어서 함수를 바로 만듦
val squareBoxedUsingUncurriedMap2 = mapBoxedUncurried(square,_:Boxed[Int])

